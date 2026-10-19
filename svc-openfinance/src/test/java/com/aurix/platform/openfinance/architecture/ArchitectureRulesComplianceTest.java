package com.aurix.platform.openfinance.architecture;

import com.aurix.platform.openfinance.extractor.BaseExtractor;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Avalia em CI as 20 regras de {@code architecture/rules/architecture-rules.yaml}
 * (RULE-001..RULE-020) contra {@code architecture.yaml}, {@code infrastructure.yaml}
 * e {@code events.yaml}, além de manter {@code infrastructure.yaml} sincronizado
 * com as migrations Flyway e as entidades JPA — Issue #22.
 *
 * <p>Convenção: cada regra {@code RULE-NNN} tem um método {@code regraNNN...}.
 * O teste {@link #todaRegraDeveTerVerificacaoCorrespondente()} falha se uma regra
 * for adicionada ao YAML sem verificação.
 */
class ArchitectureRulesComplianceTest {

    private static final String PACOTE_BASE = "com.aurix.platform.openfinance";
    private static final Pattern METODO_REGRA = Pattern.compile("^regra(\\d{3}).*");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:[\\w]+\\.)?(\\w+)",
            Pattern.CASE_INSENSITIVE);

    private static Path archDir;
    private static Map<String, Object> arquitetura;
    private static Map<String, Object> infraestrutura;
    private static Map<String, Object> eventos;
    private static List<Map<String, Object>> regras;

    @BeforeAll
    static void carregarYamls() throws IOException {
        archDir = Files.exists(Path.of("architecture/architecture.yaml"))
                ? Path.of("architecture")
                : Path.of("../architecture");
        arquitetura = carregar(archDir.resolve("architecture.yaml"));
        infraestrutura = carregar(archDir.resolve("infrastructure.yaml"));
        eventos = carregar(archDir.resolve("events.yaml"));
        regras = lista(carregar(archDir.resolve("rules/architecture-rules.yaml")), "rules");
    }

    // ─── Meta: cobertura das regras ───

    @Test
    @DisplayName("Toda regra de architecture-rules.yaml deve ter verificação em CI")
    void todaRegraDeveTerVerificacaoCorrespondente() {
        Set<String> idsYaml = regras.stream()
                .map(r -> String.valueOf(r.get("id")))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> idsTestados = Arrays.stream(getClass().getDeclaredMethods())
                .map(Method::getName)
                .map(METODO_REGRA::matcher)
                .filter(Matcher::matches)
                .map(m -> "RULE-" + m.group(1))
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(20, idsYaml.size(), "Esperadas 20 regras em architecture-rules.yaml");
        assertEquals(idsYaml, idsTestados, "Regras do YAML sem verificação (ou verificações órfãs)");
        regras.forEach(r -> {
            assertNotNull(r.get("check"), r.get("id") + " sem 'check'");
            assertTrue(List.of("CRITICAL", "HIGH", "MEDIUM").contains(r.get("severity")),
                    r.get("id") + " com severidade inválida");
        });
    }

    // ─── Consent Invariants ───

    @Test
    @DisplayName("RULE-001: nenhum acesso a dados sem AuthorizedContext")
    void regra001SemBypassDeConsentimento() {
        assertTrue(lista(componente("execution", "extraction-planner"), "consumes").contains("AuthorizedContext"));
        assertEquals(Boolean.TRUE, requisito(estagio("extraction"), "authorized-context-required"));
        assertEquals(Boolean.TRUE, caminho(arquitetura, "pipeline", "requirements", "extract", "authorizedContextRequired"));
        assertEquals("mandatory", caminho(arquitetura, "pipeline", "requirements", "authorize", "policyCheck"));
    }

    @Test
    @DisplayName("RULE-002: workflows exigem consentimento versionado")
    void regra002ConsentimentoObrigatorioParaExecucao() {
        assertTrue(lista(arquitetura, "idempotency", "workflow", "key").containsAll(List.of("consentId", "consentVersion")));
        assertTrue(lista(componente("execution", "workflow-engine"), "workflows").contains("DataExtractionWorkflow"));
        assertTrue(lista(componente("execution", "workflow-engine"), "activities").contains("ValidateConsentActivity"));
    }

    // ─── Temporal Restrictions ───

    @Test
    @DisplayName("RULE-003: Temporal não decide autorização")
    void regra003TemporalSemAutorizacao() {
        Map<String, Object> engine = componente("execution", "workflow-engine");
        assertTrue(lista(engine, "forbidden").contains("authorization_decision"));
        assertFalse(lista(engine, "allowed").contains("authorization_decision"));
    }

    @Test
    @DisplayName("RULE-004: Temporal não decide consentimento nem política regulatória")
    void regra004TemporalSemDecisaoDeConsentimento() {
        assertTrue(lista(componente("execution", "workflow-engine"), "forbidden")
                .containsAll(List.of("consent_decision", "regulatory_policy_decision")));
    }

    @Test
    @DisplayName("RULE-005: Temporal não acessa fontes diretamente")
    void regra005TemporalSemAcessoDiretoADados() {
        assertTrue(lista(componente("execution", "workflow-engine"), "forbidden").contains("direct_source_access"));
    }

    // ─── Data Traceability ───

    @Test
    @DisplayName("RULE-006: linhagem antes da publicação")
    void regra006LinhagemAntesDaPublicacao() {
        List<String> ids = idsEstagios();
        assertTrue(ids.indexOf("lineage") >= 0 && ids.indexOf("lineage") < ids.indexOf("publication"));
        assertEquals(Boolean.TRUE, requisito(estagio("publication"), "lineage-created"));
        assertTrue(lista(arquitetura, "pipeline", "requirements", "publish", "requires").contains("lineage_created"));
    }

    @Test
    @DisplayName("RULE-007: cadeia de rastreabilidade completa")
    void regra007CadeiaDeRastreabilidadeCompleta() {
        assertTrue(lista(arquitetura, "traceability", "requiredChain").containsAll(List.of(
                "consentId", "consentVersion", "resourceId", "executionPlanId", "temporalWorkflowId",
                "pipelineExecutionId", "sourceRecordId", "canonicalRecordId", "publicationId")));
    }

    // ─── Schema Evolution ───

    @Test
    @DisplayName("RULE-008: schemas BACKWARD compatíveis")
    void regra008SchemaRetrocompativel() {
        assertEquals(Boolean.TRUE, requisito(estagio("schema-validation"), "backward-compatible"));
        assertEquals(Boolean.TRUE, caminho(infraestrutura, "runtime", "messaging", "kafka", "schemaRegistry", "enabled"));
        assertEquals("BACKWARD", caminho(infraestrutura, "runtime", "messaging", "kafka", "schemaRegistry", "compatibility"));
    }

    // ─── Pipeline Requirements ───

    @Test
    @DisplayName("RULE-009: reconciliação presente")
    void regra009PipelineComReconciliacao() {
        assertNotNull(caminho(arquitetura, "planes", "execution", "components", "reconciliation"));
        assertTrue(lista(componente("execution", "workflow-engine"), "workflows").contains("ReconciliationWorkflow"));
        assertTrue(lista(infraestrutura, "runtime", "temporal", "taskQueues").contains("openfinance-reconciliation"));
    }

    @Test
    @DisplayName("RULE-010: 7 estágios do Data Plane na ordem canônica")
    void regra010EstagiosDeDadosCompletos() {
        assertEquals(List.of("extraction", "canonicalization", "schema-validation", "data-quality",
                "pii-classification", "lineage", "publication"), idsEstagios());
    }

    // ─── Security ───

    @Test
    @DisplayName("RULE-011: FAPI-Brazil obrigatório")
    void regra011FapiBrazilObrigatorio() {
        assertTrue(lista(componente("trust", "fapi-auth-server"), "responsibility").contains("fapi-brazil-compliance"));
        assertTrue(lista(arquitetura, "security", "protocols").contains("fapi-brazil"));
        assertEquals("fapi-brazil", caminho(arquitetura, "planes", "distribution", "channels", "api", "auth"));
    }

    @Test
    @DisplayName("RULE-012: tokens sender-constrained (DPoP)")
    void regra012TokensSenderConstrained() {
        assertTrue(lista(arquitetura, "security", "protocols").contains("dpop"));
        assertTrue(lista(componente("trust", "token-service"), "responsibility").contains("sender-constrained-tokens"));
        assertTrue(lista(componente("trust", "fapi-auth-server"), "responsibility").contains("dpop-validation"));
    }

    @Test
    @DisplayName("RULE-013: mTLS obrigatório")
    void regra013MtlsObrigatorio() {
        assertTrue(lista(arquitetura, "security", "protocols").contains("mtls"));
        assertEquals("istio-mtls", componente("trust", "tls-terminator").get("technology"));
        assertTrue(lista(infraestrutura, "runtime", "serviceMesh", "istio", "features").contains("mtls-strict"));
    }

    // ─── Consent Versioning / Authorized Context ───

    @Test
    @DisplayName("RULE-014: consentimento versionado, nunca mutado")
    void regra014VersaoDeConsentimentoImutavel() {
        assertTrue(regrasDeInvariantes().contains("CONSENT_VERSIONING"));
        assertTrue(lista(componente("consent", "consent-service"), "responsibility").contains("maintain_consent_versions"));
    }

    @Test
    @DisplayName("RULE-015: AuthorizedContext imutável e assinado")
    void regra015ContextoAutorizadoImutavel() {
        Map<String, Object> contexto = componente("consent", "authorized-context");
        assertEquals("immutable-artifact", contexto.get("type"));
        assertTrue(lista(contexto, "structure", "fields").containsAll(List.of("consentVersion", "signature", "dpopThumbprint")));
        assertTrue(regrasDeInvariantes().contains("IMMUTABLE_AUTHORIZED_CONTEXT"));
    }

    // ─── Cross-cutting ───

    @Test
    @DisplayName("RULE-016: observabilidade OpenTelemetry + tracing distribuído")
    void regra016ObservabilidadeObrigatoria() {
        assertEquals("opentelemetry", caminho(arquitetura, "observability", "standard"));
        assertTrue(lista(arquitetura, "observability", "requiredContext").containsAll(List.of("traceId", "correlationId", "consentId")));
        assertEquals("jaeger", caminho(arquitetura, "technology", "observability", "traces"));
        assertEquals("OTLP", caminho(infraestrutura, "runtime", "monitoring", "jaeger", "protocol"));
    }

    @Test
    @DisplayName("RULE-017: trilha de auditoria das decisões de política")
    void regra017TrilhaDeAuditoriaObrigatoria() {
        Map<String, Object> policy = componente("consent", "policy-engine");
        assertTrue(lista(policy, "responsibility").contains("audit_decisions"));
        assertTrue(lista(policy, "api").contains("GET /api/v1/policy/decisions"));
    }

    @Test
    @DisplayName("RULE-018: criptografia em repouso (AES-256-GCM) e trânsito (TLS 1.3)")
    void regra018CriptografiaEmRepousoETransito() {
        assertEquals("AES-256-GCM", caminho(arquitetura, "security", "sensitiveData", "encryptionAtRest"));
        assertEquals("TLS-1.3", caminho(arquitetura, "security", "sensitiveData", "encryptionInTransit"));
        assertEquals("mandatory", caminho(arquitetura, "security", "sensitiveData", "piiDetection"));
    }

    // ─── Distribution ───

    @Test
    @DisplayName("RULE-019: padrão de tópicos Kafka consistente em todos os YAMLs")
    void regra019NomenclaturaDeTopicosKafka() {
        Object padrao = caminho(eventos, "namingConvention", "pattern");
        assertEquals("{domain}.{entity}.{action}.{version}", padrao);
        assertEquals(padrao, caminho(arquitetura, "planes", "distribution", "channels", "kafka", "topicPattern"));
        assertEquals(padrao, caminho(infraestrutura, "runtime", "messaging", "kafka", "topics", "pattern"));
    }

    // ─── Domain Coverage ───

    @Test
    @DisplayName("RULE-020: toda capability declara pipeline e extractor implementado")
    void regra020TodosOsDominiosTemExtractors() {
        Map<String, Object> dominios = mapa(arquitetura, "domains");
        assertFalse(dominios.isEmpty());
        dominios.forEach((dominio, def) -> mapa(def, "capabilities").forEach((capability, cap) -> {
            String onde = dominio + "." + capability;
            Map<String, Object> c = mapa(cap);
            assertNotNull(c.get("pipeline"), onde + " sem pipeline");
            String extractor = (String) c.get("extractor");
            assertNotNull(extractor, onde + " sem extractor");
            try {
                Class<?> classe = Class.forName(PACOTE_BASE + ".extractor." + extractor);
                assertTrue(BaseExtractor.class.isAssignableFrom(classe), extractor + " não estende BaseExtractor");
            } catch (ClassNotFoundException e) {
                fail(onde + ": extractor " + extractor + " não existe no código");
            }
        }));
    }

    // ─── infrastructure.yaml ↔ Flyway ↔ JPA ───

    @Test
    @DisplayName("infrastructure.yaml deve listar exatamente as tabelas das migrations Flyway")
    void tabelasDaInfraestruturaDevemBaterComMigrations() throws IOException {
        Set<String> declaradas = new TreeSet<>(tabelasDeclaradas());
        assertEquals(tabelasDeclaradas().size(), declaradas.size(), "Tabelas duplicadas em infrastructure.yaml");
        assertEquals(tabelasDasMigrations(), declaradas,
                "infrastructure.yaml#runtime.databases.postgresql.tables divergente de db/migration");
    }

    @Test
    @DisplayName("Toda @Entity deve mapear uma tabela declarada em infrastructure.yaml")
    void entidadesJpaDevemMapearTabelasDeclaradas() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        Set<String> declaradas = new HashSet<>(tabelasDeclaradas());
        List<String> semTabela = new ArrayList<>();
        int total = 0;
        for (BeanDefinition bd : scanner.findCandidateComponents(PACOTE_BASE)) {
            Class<?> classe = Class.forName(bd.getBeanClassName());
            Table table = classe.getAnnotation(Table.class);
            String nome = table != null && !table.name().isBlank() ? table.name() : classe.getSimpleName();
            total++;
            if (!declaradas.contains(nome)) {
                semTabela.add(classe.getSimpleName() + " -> " + nome);
            }
        }
        assertTrue(total >= 30, "Esperadas ao menos 30 entidades JPA, encontradas " + total);
        assertTrue(semTabela.isEmpty(), "Entidades sem tabela declarada em infrastructure.yaml: " + semTabela);
    }

    // ─── Helpers ───

    private static Set<String> tabelasDasMigrations() throws IOException {
        Path migrations = archDir.resolveSibling("svc-openfinance/src/main/resources/db/migration");
        Set<String> tabelas = new TreeSet<>();
        try (Stream<Path> arquivos = Files.list(migrations)) {
            for (Path sql : arquivos.filter(p -> p.toString().endsWith(".sql")).toList()) {
                Matcher m = CREATE_TABLE.matcher(Files.readString(sql));
                while (m.find()) {
                    tabelas.add(m.group(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        assertFalse(tabelas.isEmpty(), "Nenhum CREATE TABLE encontrado em " + migrations);
        return tabelas;
    }

    private static List<String> tabelasDeclaradas() {
        return lista(infraestrutura, "runtime", "databases", "postgresql", "tables");
    }

    private static Map<String, Object> componente(String plano, String nome) {
        return mapa(arquitetura, "planes", plano, "components", nome);
    }

    private static List<Map<String, Object>> estagios() {
        return lista(arquitetura, "planes", "dataPlane", "stages");
    }

    private static List<String> idsEstagios() {
        return estagios().stream().map(e -> String.valueOf(e.get("id"))).toList();
    }

    private static Map<String, Object> estagio(String id) {
        return estagios().stream().filter(e -> id.equals(e.get("id"))).findFirst()
                .orElseThrow(() -> new AssertionError("Estágio ausente no dataPlane: " + id));
    }

    /** Os requisitos de estágio são uma lista de mapas de uma chave ({@code - idempotent: true}). */
    private static Object requisito(Map<String, Object> estagio, String chave) {
        List<Map<String, Object>> reqs = lista(estagio, "requirements");
        return reqs.stream().filter(r -> r.containsKey(chave)).map(r -> r.get(chave)).findFirst().orElse(null);
    }

    private static Set<String> regrasDeInvariantes() {
        List<Map<String, Object>> invs = lista(arquitetura, "invariants");
        return invs.stream().map(i -> String.valueOf(i.get("rule"))).collect(Collectors.toSet());
    }

    private static Object caminho(Object raiz, String... chaves) {
        Object atual = raiz;
        for (String chave : chaves) {
            if (!(atual instanceof Map<?, ?> m) || !m.containsKey(chave)) {
                fail("Caminho ausente: " + String.join(".", chaves) + " (falhou em '" + chave + "')");
            }
            atual = ((Map<?, ?>) atual).get(chave);
        }
        return atual;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object raiz, String... chaves) {
        Object valor = chaves.length == 0 ? raiz : caminho(raiz, chaves);
        assertInstanceOf(Map.class, valor, "Esperado mapa em " + String.join(".", chaves));
        return (Map<String, Object>) valor;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> lista(Object raiz, String... chaves) {
        Object valor = caminho(raiz, chaves);
        assertInstanceOf(List.class, valor, "Esperada lista em " + String.join(".", chaves));
        return (List<T>) valor;
    }

    private static Map<String, Object> carregar(Path arquivo) throws IOException {
        try (Reader reader = Files.newBufferedReader(arquivo)) {
            Map<String, Object> conteudo = new Yaml().load(reader);
            assertNotNull(conteudo, "YAML vazio: " + arquivo);
            return conteudo;
        }
    }
}
