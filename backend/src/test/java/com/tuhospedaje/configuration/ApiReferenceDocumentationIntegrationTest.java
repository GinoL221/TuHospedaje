package com.tuhospedaje.configuration;

import com.tuhospedaje.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Anti-drift guard for {@code docs/markdown/api-reference.md}.
 *
 * <p>The markdown file documents every controller handler in a fixed-format endpoint
 * table ({@code | Method | Path | Access | Request | Response | Statuses |}). This test
 * reads the generated OpenAPI spec served by {@code /v3/api-docs} (MockMvc, no running
 * server) and asserts the table and the spec agree:
 *
 * <ol>
 *   <li>every operation in the spec has a row in the table, and every row exists in the
 *       spec — mismatches are listed pair by pair;</li>
 *   <li>every status the spec documents for an operation also appears in that row's
 *       {@code Statuses} cell;</li>
 *   <li>the row count equals the operation count, so a duplicated row cannot hide.</li>
 * </ol>
 *
 * <p>When this test fails, a maintainer should update
 * {@code docs/markdown/api-reference.md} (and, when a handler or status changed, the
 * OpenAPI annotations behind the spec). The failure messages name the exact
 * {@code METHOD path} pairs to touch.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiReferenceDocumentationIntegrationTest extends AbstractIntegrationTest {

    /** Resolved from {@code backend/} (Maven's working directory) to the repo root doc. */
    private static final Path API_REFERENCE = Paths.get("..", "docs", "markdown", "api-reference.md");

    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    /** Matches the backticked numeric statuses of a row's {@code Statuses} cell. */
    private static final Pattern STATUS_CODE = Pattern.compile("\\b\\d{3}\\b");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("docs/markdown/api-reference.md endpoint table matches the generated OpenAPI spec")
    void endpointTableMatchesGeneratedSpec() throws Exception {
        assertThat(API_REFERENCE)
                .as("docs/markdown/api-reference.md must exist relative to the backend module "
                        + "(the test resolves ../docs/markdown/api-reference.md from the repo root)")
                .exists();

        String specJson = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Map<String, Set<String>> specOps = readSpecOperations(jsonMapper.readTree(specJson));

        TableParse table = readEndpointTable(Files.readAllLines(API_REFERENCE));

        // 1. One-to-one operation coverage.
        Set<String> missing = new TreeSet<>(specOps.keySet());
        missing.removeAll(table.rows().keySet());
        assertThat(missing)
                .as("Operations documented in /v3/api-docs but missing from the endpoint table of "
                        + "docs/markdown/api-reference.md. Add one table row for each pair listed below "
                        + "(| Method | Path | Access | Request | Response | Statuses |).")
                .isEmpty();
        Set<String> extra = new TreeSet<>(table.rows().keySet());
        extra.removeAll(specOps.keySet());
        assertThat(extra)
                .as("Endpoint-table rows in docs/markdown/api-reference.md that do not exist in the "
                        + "generated spec. Remove each row below, or restore the OpenAPI annotation on "
                        + "the corresponding controller handler if the endpoint still exists.")
                .isEmpty();

        // 2. Every spec status must appear in the matching row's Statuses cell.
        List<String> statusGaps = new ArrayList<>();
        for (Map.Entry<String, Set<String>> op : specOps.entrySet()) {
            Set<String> documented = table.rows().get(op.getKey());
            Set<String> gap = new TreeSet<>(op.getValue());
            gap.removeAll(documented);
            if (!gap.isEmpty()) {
                statusGaps.add(op.getKey() + ": the spec documents " + op.getValue()
                        + " but the row's Statuses cell lacks " + gap);
            }
        }
        assertThat(statusGaps)
                .as("Status cells out of date in docs/markdown/api-reference.md. Update the Statuses "
                        + "cell of each operation below to include the missing statuses.")
                .isEmpty();

        // 3. Row count equals the operation count (catches duplicated rows).
        assertThat(table.duplicateOps())
                .as("Duplicated rows in the endpoint table of docs/markdown/api-reference.md. Keep "
                        + "exactly one row per METHOD path pair.")
                .isEmpty();
        assertThat(table.rowCount())
                .as("The endpoint table of docs/markdown/api-reference.md must have exactly one row "
                        + "per generated-spec operation (a count above %d means duplicated rows).",
                        specOps.size())
                .isEqualTo(specOps.size());
    }

    /** {@code "METHOD path"} → the response statuses the spec documents for it. */
    private Map<String, Set<String>> readSpecOperations(JsonNode spec) {
        Map<String, Set<String>> ops = new TreeMap<>();
        JsonNode paths = spec.path("paths");
        paths.properties().forEach(pathEntry -> {
            String path = pathEntry.getKey();
            pathEntry.getValue().properties().forEach(itemEntry -> {
                String method = itemEntry.getKey().toUpperCase(Locale.ROOT);
                if (!HTTP_METHODS.contains(method)) {
                    return; // path-item level keys such as "parameters" or "summary"
                }
                Set<String> statuses = new TreeSet<>();
                itemEntry.getValue().path("responses").properties()
                        .forEach(responseEntry -> statuses.add(responseEntry.getKey()));
                ops.put(method + " " + path, statuses);
            });
        });
        return ops;
    }

    private record TableParse(int rowCount, Map<String, Set<String>> rows, List<String> duplicateOps) {
    }

    /**
     * Parses the endpoint table defensively: only lines starting with {@code |} are
     * considered, the header and separator rows are skipped because their first cell is
     * not an HTTP method, and so are any other table-like lines whose first cell is not
     * an HTTP method.
     */
    private TableParse readEndpointTable(List<String> lines) {
        Map<String, Set<String>> rows = new LinkedHashMap<>();
        List<String> duplicates = new ArrayList<>();
        int rowCount = 0;
        for (String line : lines) {
            if (!line.startsWith("|")) {
                continue;
            }
            String[] cells = line.replaceAll("^\\||\\|$", "").split("\\|", -1);
            if (cells.length < 6) {
                continue;
            }
            String method = cells[0].trim();
            if (!HTTP_METHODS.contains(method)) {
                continue; // header ("Method"), separator ("---"), or prose rows
            }
            rowCount++;
            String op = method + " " + cells[1].trim();
            if (rows.containsKey(op)) {
                duplicates.add(op);
            }
            Set<String> statuses = new LinkedHashSet<>();
            Matcher matcher = STATUS_CODE.matcher(cells[5]);
            while (matcher.find()) {
                statuses.add(matcher.group());
            }
            rows.put(op, statuses);
        }
        return new TableParse(rowCount, rows, duplicates);
    }
}
