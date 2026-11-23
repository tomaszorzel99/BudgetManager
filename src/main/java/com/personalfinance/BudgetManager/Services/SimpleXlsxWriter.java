package com.personalfinance.BudgetManager.Services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Minimalny zapis pliku .xlsx (Office Open XML) bez zewnętrznych bibliotek:
 * pogrubiony nagłówek, zamrożony pierwszy wiersz, kwoty z separatorem tysięcy i daty.
 */
public class SimpleXlsxWriter {

    public enum ColumnType { TEXT, MONEY, DATE, NUMBER }

    public record Column(String header, double width, ColumnType type) { }

    private record Sheet(String name, List<Column> columns, List<Object[]> rows) { }

    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);
    private static final int STYLE_HEADER = 1, STYLE_MONEY = 2, STYLE_DATE = 3, STYLE_NUMBER = 4;

    private final List<Sheet> sheets = new ArrayList<>();

    public SimpleXlsxWriter addSheet(String name, List<Column> columns, List<Object[]> rows) {
        String safe = name.replaceAll("[\\\\/?*\\[\\]:]", " ");
        sheets.add(new Sheet(safe.length() > 31 ? safe.substring(0, 31) : safe, columns, rows));
        return this;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", contentTypes());
            put(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>""");
            put(zip, "xl/workbook.xml", workbook());
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            put(zip, "xl/styles.xml", STYLES);
            for (int i = 0; i < sheets.size(); i++) {
                put(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheetXml(sheets.get(i)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String contentTypes() {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                """);
        for (int i = 1; i <= sheets.size(); i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n");
        }
        return sb.append("</Types>").toString();
    }

    private String workbook() {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>""");
        for (int i = 0; i < sheets.size(); i++) {
            sb.append("<sheet name=\"").append(xml(sheets.get(i).name())).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return sb.append("</sheets></workbook>").toString();
    }

    private String workbookRels() {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                """);
        int i = 1;
        for (; i <= sheets.size(); i++) {
            sb.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>\n");
        }
        sb.append("<Relationship Id=\"rId").append(i)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n");
        return sb.append("</Relationships>").toString();
    }

    private String sheetXml(Sheet sheet) {
        StringBuilder sb = new StringBuilder(4096 + sheet.rows().size() * 200);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
                .append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
                .append("<cols>");
        List<Column> cols = sheet.columns();
        for (int c = 0; c < cols.size(); c++) {
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                    .append("\" width=\"").append(cols.get(c).width()).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");
        sb.append("<row r=\"1\">");
        for (int c = 0; c < cols.size(); c++) {
            inlineString(sb, ref(c, 1), cols.get(c).header(), STYLE_HEADER);
        }
        sb.append("</row>");
        int r = 2;
        for (Object[] row : sheet.rows()) {
            sb.append("<row r=\"").append(r).append("\">");
            for (int c = 0; c < cols.size() && c < row.length; c++) {
                Object v = row[c];
                if (v == null) continue;
                String ref = ref(c, r);
                if (v instanceof LocalDate d) {
                    number(sb, ref, String.valueOf(ChronoUnit.DAYS.between(EXCEL_EPOCH, d)), STYLE_DATE);
                } else if (v instanceof BigDecimal b) {
                    number(sb, ref, b.toPlainString(), cols.get(c).type() == ColumnType.NUMBER ? STYLE_NUMBER : STYLE_MONEY);
                } else if (v instanceof Number n) {
                    number(sb, ref, n.toString(), cols.get(c).type() == ColumnType.MONEY ? STYLE_MONEY : STYLE_NUMBER);
                } else {
                    inlineString(sb, ref, v.toString(), 0);
                }
            }
            sb.append("</row>");
            r++;
        }
        return sb.append("</sheetData></worksheet>").toString();
    }

    private static void inlineString(StringBuilder sb, String ref, String text, int style) {
        sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"");
        if (style > 0) sb.append(" s=\"").append(style).append('"');
        sb.append("><is><t xml:space=\"preserve\">").append(xml(text)).append("</t></is></c>");
    }

    private static void number(StringBuilder sb, String ref, String value, int style) {
        sb.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\"><v>").append(value).append("</v></c>");
    }

    static String ref(int col, int row) {
        StringBuilder name = new StringBuilder();
        int n = col + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            name.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return name.append(row).toString();
    }

    static String xml(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> {
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch);
                }
            }
        }
        return sb.toString();
    }

    private static final String STYLES = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <numFmts count="1"><numFmt numFmtId="164" formatCode="yyyy\\-mm\\-dd"/></numFmts>
            <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
            <fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFF2F4F7"/><bgColor indexed="64"/></patternFill></fill></fills>
            <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
            <cellXfs count="5">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
            <xf numFmtId="4" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            </cellXfs>
            <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
            </styleSheet>""";
}
