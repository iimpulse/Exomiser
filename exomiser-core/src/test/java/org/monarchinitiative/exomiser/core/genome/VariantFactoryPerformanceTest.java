/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2021 Queen Mary University of London.
 * Copyright (c) 2012-2016 Charité Universitätsmedizin Berlin and Genome Research Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.monarchinitiative.exomiser.core.genome;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.data.JannovarData;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.core.genome.dao.AllelePropertiesDao;
import org.monarchinitiative.exomiser.core.genome.dao.AllelePropertiesDaoAdapter;
import org.monarchinitiative.exomiser.core.genome.dao.AllelePropertiesDaoMvStore;
import org.monarchinitiative.exomiser.core.genome.jannovar.JannovarDataSourceLoader;
import org.monarchinitiative.exomiser.core.model.AlleleProtoAdaptor;
import org.monarchinitiative.exomiser.core.model.ChromosomalRegionIndex;
import org.monarchinitiative.exomiser.core.model.VariantAnnotation;
import org.monarchinitiative.exomiser.core.model.VariantEvaluation;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencyData;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicityData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicitySource;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class VariantFactoryPerformanceTest {

    Logger logger = LoggerFactory.getLogger(VariantFactoryPerformanceTest.class);

    /**
     * Comparative performance test for loading a full genome. Ignored by default as this takes a few minutes.
     */
    @Disabled("Performance test - won't run on CI server")
    @Test
    public void testGenome() {

        VariantAnnotator stubVariantAnnotator = new StubVariantAnnotator();
        VariantFactory stubAnnotationVariantFactory = new VariantFactoryImpl(stubVariantAnnotator, Path.of("src/test/resources/multiSampleWithProbandHomRef.vcf"));

        //warm-up
        for (int i = 0; i < 1000; i++) {
            countVariants(stubAnnotationVariantFactory, new StubAllelePropertiesDao());
        }

        Path vcfPath = Paths.get("/home/hhx640/Documents/exomiser-cli-dev/examples/NA19722_601952_AUTOSOMAL_RECESSIVE_POMP_13_29233225_5UTR_38.vcf.gz");
//        Path vcfPath = Paths.get("C:/Users/hhx640/Documents/exomiser-cli-dev/examples/Pfeiffer-quartet.vcf.gz");
//        Path vcfPath = Paths.get("C:/Users/hhx640/Documents/exomiser-cli-dev/examples/NA19240.sniffles.PB.vcf");
//        Path vcfPath = Paths.get("C:/Users/hhx640/Documents/exomiser-cli-dev/examples/example_sv.vcf");

        System.out.println("Read variants with stub annotations, stub data - baseline file reading and VariantEvaluation creation");
        runPerfTest(4, new VariantFactoryImpl(stubVariantAnnotator, vcfPath), new StubAllelePropertiesDao());

        JannovarData jannovarData = loadJannovarData();
        VariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(GenomeAssembly.HG19, jannovarData, ChromosomalRegionIndex
                .empty());
        VariantFactory jannovarVariantFactory = new VariantFactoryImpl(jannovarVariantAnnotator, vcfPath);

        System.out.println("Read variants with real annotations, stub data");
        runPerfTest(4, jannovarVariantFactory, new StubAllelePropertiesDao());
//
//
//        VariantAnnotator vittVariantAnnotator = new VittAnnotatator(GenomeAssembly.HG19, loadTranscriptIndex(GenomeAssembly.HG19, jannovarData));
//        VariantFactory vittVariantFactory = new VariantFactoryImpl(vittVariantAnnotator, vcfPath);
//
//        System.out.println("Read variants with real annotations, stub data");
//        runPerfTest(4, vittVariantFactory, new StubAllelePropertiesDao());

        // This should take about 10-15 mins as it annotates every variant in the file from the database
//        System.out.println("Read variants with real annotations, real data");
//        runPerfTest(1, jannovarVariantFactory, allelePropertiesDao());

    }

    //    @Disabled("performance test")
//    @Test
//    void rawAnnotatorPerformance() {
//        Path vcfPath = Paths.get("/home/hhx640/Documents/exomiser-cli-dev/examples/NA19722_601952_AUTOSOMAL_RECESSIVE_POMP_13_29233225_5UTR_38.vcf.gz");
//        VariantTrimmer vcfStyleTrimmer = VariantTrimmer.leftShiftingTrimmer(VariantTrimmer.retainingCommonBase());
//        VariantTrimmer hgvsStyleTrimmer = VariantTrimmer.rightShiftingTrimmer(VariantTrimmer.removingCommonBase());
//        VariantContextConverter variantContextConverter = VariantContextConverter.of(GenomeAssembly.HG19.genomicAssembly(), hgvsStyleTrimmer);
//
//        JannovarData jannovarData = loadJannovarData();
//        JannovarAnnotationService jannovarAnnotationService = new JannovarAnnotationService(jannovarData);
//
//        logger.info("Testing VCF read time");
//        Instant vcfReadStart = Instant.now();
//        long alleles = VcfFiles.readVariantContexts(vcfPath)
//                .flatMap(vc -> vc.getAlleles().stream())
//                .count();
//        Instant vcfReadEnd = Instant.now();
//        Duration vcfReadTime = Duration.between(vcfReadStart, vcfReadEnd);
//        logger.info("Read {} alleles in {}", alleles, vcfReadTime);
//
//        logger.info("Testing VCF read time with svart conversion");
//        Instant svartConvertStart = Instant.now();
//        long svartAlleles = VcfFiles.readVariantContexts(vcfPath)
//                .flatMap(vc ->
//                        vc.getAlleles().stream().map(allele -> variantContextConverter.convertToVariant(vc, allele))
//                )
//                .filter(variant -> variant.variantType() == VariantType.SNV)
//                .count();
//        Instant svartConvertEnd = Instant.now();
//        Duration svartConvertTime = Duration.between(svartConvertStart, svartConvertEnd);
//        logger.info("Read and converted {} alleles in {}", svartAlleles, svartConvertTime);
//
//        logger.info("Annotating with Jannovar");
//        JannovarVariantEffectCounter jannovarVariantEffectCounter = new JannovarVariantEffectCounter();
//        Instant jannovarStart = Instant.now();
//        long jannovarAnnotations = VcfFiles.readVariantContexts(vcfPath)
//                .flatMap(vc ->
//                        vc.getAlleles().stream().map(allele -> variantContextConverter.convertToVariant(vc, allele))
//                        )
//                .filter(variant -> variant.variantType() == VariantType.SNV)
//                .map(variant -> jannovarAnnotationService
//                        .annotateVariant(variant.contigName(), variant.start(), variant.ref(), variant.alt()))
////                .peek(variantAnnotation -> jannovarVariantEffectCounter.countEffect(variantAnnotation.getHighestImpactEffect()))
//                .count();
//        Instant jannovarEnd = Instant.now();
//        Duration jannovarDuration = Duration.between(jannovarEnd, jannovarStart);
//        logger.info("Finished annotating with Jannovar. {} variants, {} effects took {} sec ({} without VCF read time)", jannovarAnnotations, jannovarVariantEffectCounter.total(), jannovarDuration, jannovarDuration.minus(svartConvertTime));
//        jannovarVariantEffectCounter.getEffectCounts().forEach((variantEffect, count) -> {
//            if (count != 0) {
//                System.out.println(variantEffect + ": " + count);
//            }
//        });
//
//        org.monarchinitiative.vitt.VariantAnnotator vittVariantAnnotator = new org.monarchinitiative.vitt.VariantAnnotator(loadTranscriptIndex(GenomeAssembly.HG19, jannovarData));
//        logger.info("Annotating with vitt");
//        VittVariantEffectCounter vittVariantEffectCounter = new VittVariantEffectCounter();
//        Instant vittStart = Instant.now();
//        long vittAnnotations = VcfFiles.readVariantContexts(vcfPath)
//                .flatMap(vc ->
//                        vc.getAlleles().stream().map(allele -> variantContextConverter.convertToVariant(vc, allele))
//                )
//                .filter(variant -> variant.variantType() == VariantType.SNV)
//                .map(vittVariantAnnotator::annotate)
//                .peek(variantAnnotation -> vittVariantEffectCounter.countEffect(variantAnnotation.highestImpactEffect()))
//                .count();
//
//        Instant vittEnd = Instant.now();
//        Duration vittDuration = Duration.between(vittStart, vittEnd);
//        logger.info("Finished annotating with vitt. {} variants, {} effects took {} sec ({} without VCF read time)", vittAnnotations, vittVariantEffectCounter.total(), vittDuration, vittDuration.minus(svartConvertTime));
//        vittVariantEffectCounter.getEffectCounts().forEach((variantEffect, count) -> {
//            if (count != 0) {
//                System.out.println(variantEffect + ": " + count);
//            }
//        });
////        logger.info("Vitt took {} less time - {} total having removed VCF read time", vittDuration.minus(jannovarDuration), vittDuration.minus(svartConvertTime));
//    }
//
//    @Disabled("integration test")
//    @Test
//    void annotatorComparison() {
//        Path vcfPath = Paths.get("/home/hhx640/Documents/exomiser-cli-dev/examples/NA19722_601952_AUTOSOMAL_RECESSIVE_POMP_13_29233225_5UTR_38.vcf.gz");
//        VariantContextConverter variantContextConverter = VariantContextConverter.of(GenomeAssembly.HG19.genomicAssembly(), VariantTrimmer.leftShiftingTrimmer(VariantTrimmer.retainingCommonBase()));
//
//        JannovarData jannovarData = loadJannovarData();
//        JannovarAnnotationService jannovarAnnotationService = new JannovarAnnotationService(jannovarData);
//        GenomicRegionIndex<Transcript> transcriptIndex = loadTranscriptIndex(GenomeAssembly.HG19, jannovarData);
//        org.monarchinitiative.vitt.VariantAnnotator vittVariantAnnotator = new org.monarchinitiative.vitt.VariantAnnotator(transcriptIndex);
//        logger.info("{} Jannovar transcripts, {} Vitt transcripts", jannovarData.getTmByAccession().size(), transcriptIndex.size());
//
//        Iterator<VariantContext> variantContextIterator = VcfFiles.readVariantContexts(vcfPath).iterator();
//        while (variantContextIterator.hasNext()) {
//            VariantContext variantContext = variantContextIterator.next();
//            for (Allele allele : variantContext.getAlternateAlleles()) {
//                Variant variant = variantContextConverter.convertToVariant(variantContext, allele);
//                if (variant.variantType() == VariantType.SNV) {
//                    de.charite.compbio.jannovar.annotation.VariantAnnotations jannovarAnnotations = jannovarAnnotationService.annotateVariant(variant.contigName(), variant.start(), variant.ref(), variant.alt());
//                    List<String> jannovarTranscriptAccessions = jannovarAnnotations.getAnnotations().stream().map(annotation -> annotation.getTranscript().getAccession()).sorted().collect(Collectors.toList());
//                    Set<VariantEffect> jannovarEffects = jannovarAnnotations.getAnnotations().stream()
//                            .flatMap(annotation -> annotation.getEffects().stream().map(effect -> VariantEffect.valueOf(effect.toString())))
//                            .collect(Sets.toImmutableEnumSet());
//
//                    org.monarchinitiative.vitt.VariantAnnotation vittAnnotations = vittVariantAnnotator.annotate(variant);
//                    List<String> vittTranscriptAccessions = vittAnnotations.transcriptAnnotations().stream().map(transcriptAnnotation -> transcriptAnnotation.transcript().accession()).sorted().collect(Collectors.toList());
//                    Set<VariantEffect> vittEffects = vittAnnotations.variantEffects();
//                    if (!jannovarTranscriptAccessions.equals(vittTranscriptAccessions)) {
//                        logger.info("{} Jannovar: {} Vitt: {}", variant, jannovarTranscriptAccessions, vittTranscriptAccessions);
//                    }
//                    if (!jannovarEffects.equals(vittEffects)) {
//                        logger.info("{}", variant);
//                        jannovarAnnotations.getAnnotations().stream().sorted(Comparator.comparing(annotation -> annotation.getTranscript().getAccession())).forEach(annotation -> logger.info("Jannovar: {}, {}, {}", annotation.getTranscript().getAccession(), annotation.getEffects(), annotation.getTranscript()));
//                        vittAnnotations.transcriptAnnotations().stream().sorted(Comparator.comparing(annotation -> annotation.transcript().accession())).forEach(annotation -> logger.info("Vitt: {}, {}, {}", annotation.transcript().accession(), annotation.variantEffects(), annotation.transcript()));
//                    }
//                }
//            }
//        }
//    }
//
//    private static class JannovarVariantEffectCounter {
//        private final int[] effectCounts;
//
//        public JannovarVariantEffectCounter() {
//            effectCounts = new int[de.charite.compbio.jannovar.annotation.VariantEffect.values().length];
//            Arrays.fill(effectCounts, 0);
//        }
//
//        public void countEffect(de.charite.compbio.jannovar.annotation.VariantEffect variantEffect) {
//            effectCounts[variantEffect.ordinal()]++;
//        }
//
//        public Map<de.charite.compbio.jannovar.annotation.VariantEffect, Integer> getEffectCounts() {
//            Map<de.charite.compbio.jannovar.annotation.VariantEffect, Integer> counts = new EnumMap<>(de.charite.compbio.jannovar.annotation.VariantEffect.class);
//            for (var variantEffect : de.charite.compbio.jannovar.annotation.VariantEffect.values()) {
//                counts.put(variantEffect, effectCounts[variantEffect.ordinal()]);
//            }
//            return counts;
//        }
//
//        public long total() {
//            long total = 0;
//            for (int i = 0; i < effectCounts.length; i++) {
//                total += effectCounts[i];
//            }
//            return total;
//        }
//    }
//
//    private static class VittVariantEffectCounter {
//        private final int[] effectCounts;
//
//        public VittVariantEffectCounter() {
//            effectCounts = new int[VariantEffect.values().length];
//            Arrays.fill(effectCounts, 0);
//        }
//
//        public void countEffect(VariantEffect variantEffect) {
//            effectCounts[variantEffect.ordinal()]++;
//        }
//
//        public Map<VariantEffect, Integer> getEffectCounts() {
//            Map<VariantEffect, Integer> counts = new EnumMap<>(VariantEffect.class);
//            for (VariantEffect variantEffect : VariantEffect.values()) {
//                counts.put(variantEffect, effectCounts[variantEffect.ordinal()]);
//            }
//            return counts;
//        }
//
//        public long total() {
//            long total = 0;
//            for (int i = 0; i < effectCounts.length; i++) {
//                total += effectCounts[i];
//            }
//            return total;
//        }
//    }
//
//    private GenomicRegionIndex<Transcript> loadTranscriptIndex(GenomeAssembly genomeAssembly, JannovarData jannovarData) {
//        List<Transcript> transcripts = new ArrayList<>(jannovarData.getTmByAccession().size());
//
//        for (TranscriptModel transcriptModel : jannovarData.getTmByAccession().values()) {
//            Contig contig = genomeAssembly.getContigById(transcriptModel.getChr());
//            Strand strand = transcriptModel.getStrand() == de.charite.compbio.jannovar.reference.Strand.FWD ? Strand.POSITIVE : Strand.NEGATIVE;
//            try {
//                Transcript transcript = DefaultTranscript.builder()
//                        .with(contig, strand, CoordinateSystem.LEFT_OPEN, Position.of(transcriptModel.getTXRegion().getBeginPos()), Position.of(transcriptModel.getTXRegion().getEndPos()))
//                        .sequence(transcriptModel.getSequence())
//                        .accession(transcriptModel.getAccession())
//                        .geneId(transcriptModel.getGeneID())
//                        .geneSymbol(transcriptModel.getGeneSymbol())
//                        .cds(Position.of(transcriptModel.getCDSRegion().getBeginPos()), Position.of(transcriptModel.getCDSRegion().getEndPos()))
//                        .exons(transcriptModel.getExonRegions().stream()
//                                .map(exon -> Exon.of(contig, strand, CoordinateSystem.LEFT_OPEN, Position.of(exon.getBeginPos()), Position.of(exon.getEndPos())))
//                                .collect(Collectors.toList()))
//                        .build();
//                transcripts.add(transcript);
//            } catch (CoordinatesOutOfBoundsException coordinatesOutOfBoundsException) {
//                // ignore these - need to fix the MT length for UCSC hg19
//            }
//        }
//
//        Map<String, Transcript> transcriptMap = transcripts.stream().collect(toMap(Transcript::accession, Function.identity()));
//
//        return GenomicRegionIndex.of(transcripts);
//    }
//
//    private static final class VittAnnotatator implements VariantAnnotator {
//
//        private final GenomeAssembly genomeAssembly;
//        private final GenomicRegionIndex<Transcript> transcriptIndex;
//
//        org.monarchinitiative.vitt.VariantAnnotator vittVariantAnnotator;
//
//        public VittAnnotatator(GenomeAssembly genomeAssembly, GenomicRegionIndex<Transcript> transcriptIndex) {
//            this.genomeAssembly = genomeAssembly;
//            this.transcriptIndex = transcriptIndex;
//            this.vittVariantAnnotator = new org.monarchinitiative.vitt.VariantAnnotator(transcriptIndex);
//        }
//
//        @Override
//        public GenomeAssembly genomeAssembly() {
//            return genomeAssembly;
//        }
//
//        @Override
//        public List<VariantAnnotation> annotate(@Nullable Variant variant) {
//            org.monarchinitiative.vitt.VariantAnnotation variantAnnotation = vittVariantAnnotator.annotate(variant);
////            if (variantAnnotation.hasAnnotation()) {
////                System.out.println(variantAnnotation);
////            }
////            System.out.println(variantAnnotation);
//            return List.of(VariantAnnotation.builder().with(variant).build());
//        }
//
//    }
//
    @Disabled
    @Test
    void testVariant() {
        VariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(GenomeAssembly.HG19, loadJannovarData(), ChromosomalRegionIndex
                .empty());

        VcfReader vcfReader = TestVcfReader.builder()
                .samples("Sample")
                .vcfLines(
                        "3       38626082        esv3595913      G       <INS:ME:LINE1>  100     PASS    CS=L1_umary;DP=18522;MEINFO=LINE1,3,6014,+;NS=2504;SVLEN=6011;SVTYPE=LINE1;TSD=AAAACAGAATGAGTAAATAATG;VT=SV        GT     1|0",
                        "3       37241307        gnomAD_v2_INS_3_22085   N       <INS>   275     PASS    END=112856057;SVTYPE=INS;CHR2=3;SVLEN=51;ALGORITHMS=delly,manta;EVIDENCE=SR;PROTEIN_CODING__NEAREST_TSS=LRRFIP2;PROTEIN_CODING__INTERGENIC;AN=20154;AC=59;AF=0.002927    GT  1/0",
                        "3       38626065        gnomAD_v2_INS_3_22148   N       <INS>   729     PASS    END=38626082;SVTYPE=INS;CHR2=3;SVLEN=6018;ALGORITHMS=melt;EVIDENCE=SR;PESR_GT_OVERDISPERSION;PROTEIN_CODING__INTRONIC=SCN5A;AN=21476;AC=6561;AF=0.305504   GT    0/1",
                        "22      19906885        esv3647281      C       <INS:ME:ALU>    100     PASS    CS=ALU_umary;DP=22624;MEINFO=AluUndef,4,259,+;NS=2504;SVLEN=255;SVTYPE=ALU;TSD=null;VT=SV GT      1|0",
                        "22      19907202        gnomAD_v2_INS_22_120078 N       <INS>   330     PASS    END=19907252;SVTYPE=INS;CHR2=22;SVLEN=51;ALGORITHMS=manta;EVIDENCE=SR;PROTEIN_CODING__INTRONIC=TXNRD2;AN=17078;AC=1633;AF=0.09562  GT  0/1")
                .build();

        VariantFactory variantFactory = new VariantFactoryImpl(jannovarVariantAnnotator, vcfReader);

        variantFactory.createVariantEvaluations().forEach(printVariant());
    }

    @Disabled
    @Test
    void testSingleVariant() {
        VariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(GenomeAssembly.HG19, loadJannovarData(), ChromosomalRegionIndex
                .empty());
        VcfReader vcfReader = TestVcfReader.builder().samples("Sample")
                .vcfLines(
                        "17       45221273        .      A       C  100     PASS    NM_001256;MISSENSE        GT     1/0",
                        "17       45221318        .      A       C  100     PASS    NM_001256;MISSENSE        GT     1/0",
                        "17       45221303        .      G       A  100     PASS    NM_001256;SYNONYMOUS        GT     1/0",
                        "17       45234632        .      A       G  100     PASS    NM_001256;SYNONYMOUS        GT     1/0",
                        "17       45221299        .      A       G  100     PASS    NM_001256;MISSENSE        GT     1/0",
                        "17       45233654        .      T       G  100     PASS    NM_001256;MISSENSE        GT     1/0",
                        "17       45232137        .      T       C  100     PASS    NM_001256;MISSENSE        GT     1/0"
                )
                .build();
        VariantFactory variantFactory = new VariantFactoryImpl(jannovarVariantAnnotator, vcfReader);


        AllelePropertiesDaoMvStore allelePropertiesDaoMvStore = new AllelePropertiesDaoMvStore(MVStore.open("/home/hhx640/Documents/exomiser-data/1902_hg19/1902_hg19_variants.mv.db"));
        AllelePropertiesDaoAdapter allelePropertiesDao = new AllelePropertiesDaoAdapter(allelePropertiesDaoMvStore);
        VariantDataService variantDataService = VariantDataServiceImpl.builder()
                .defaultFrequencyDao(allelePropertiesDao)
                .defaultPathogenicityDao(allelePropertiesDao)
                .build();

        variantFactory.createVariantEvaluations()
                .forEach(vareval -> {
                    FrequencyData frequencyData = variantDataService.getVariantFrequencyData(vareval, FrequencySource.ALL_EXTERNAL_FREQ_SOURCES);
                    PathogenicityData pathogenicityData = variantDataService.getVariantPathogenicityData(vareval, EnumSet
                            .of(PathogenicitySource.SIFT, PathogenicitySource.POLYPHEN, PathogenicitySource.MUTATION_TASTER));
                    vareval.setFrequencyData(frequencyData);
                    vareval.setPathogenicityData(pathogenicityData);
                    printVariant().accept(vareval);
                });

    }

    private Consumer<VariantEvaluation> printVariant() {
        return variantEvaluation -> System.out.printf("%d:%d-%d %s>%s length:%d %s %s %s  %s %s score:%f freq:%f (max AF:%f) path:%f (%s)%n",
                variantEvaluation.contigId(), variantEvaluation.start(), variantEvaluation.end(),
                variantEvaluation.ref(), variantEvaluation.alt(),
                variantEvaluation.changeLength(), variantEvaluation.variantType(), variantEvaluation.getVariantEffect(),
                variantEvaluation.getGeneSymbol(),
                variantEvaluation.getTranscriptAnnotations().get(0).getAccession(),
                variantEvaluation.getTranscriptAnnotations().get(0).getHgvsCdna(),
                variantEvaluation.getVariantScore(),
                variantEvaluation.getFrequencyScore(), variantEvaluation.getFrequencyData().getMaxFreq(),
                variantEvaluation.getPathogenicityScore(), variantEvaluation.getPathogenicityData().getPredictedPathogenicityScores()
        );
    }

    private void runPerfTest(int numIterations, VariantFactory variantFactory, AllelePropertiesDao allelePropertiesDao) {
        for (int i = 0; i < numIterations; i++) {
            Instant start = Instant.now();

            long numVariants = countVariants(variantFactory, allelePropertiesDao);

            Duration duration = Duration.between(start, Instant.now());
            long ms = duration.toMillis();
            System.out.printf("Read %d alleles in %dm %ds %dms (%d ms)%n", numVariants, (ms / 1000) / 60 % 60, ms / 1000 % 60, ms % 1000, ms);
        }
    }

    private long countVariants(VariantFactory variantFactory, AllelePropertiesDao allelePropertiesDao) {
        return variantFactory.createVariantEvaluations()
                .map(annotateVariant(allelePropertiesDao))
                .count();
    }

    private Function<VariantEvaluation, VariantEvaluation> annotateVariant(AllelePropertiesDao allelePropertiesDao) {
        AtomicLong totalSeekTime = new AtomicLong(0);
        AtomicInteger count = new AtomicInteger(0);
        return variantEvaluation -> {
            Instant start = Instant.now();
            AlleleProto.AlleleProperties alleleProperties = allelePropertiesDao.getAlleleProperties(variantEvaluation);
            Instant end = Instant.now();
            totalSeekTime.getAndAdd(Duration.between(start, end).toMillis());
            int current = count.incrementAndGet();
            if (current % 100_000 == 0) {
                double aveSeekTime = (double) totalSeekTime.get() / (double) current;
//                System.out.printf("%s variants in %d ms - ave seek time %3f ms/variant%n", current, totalSeekTime.get(), aveSeekTime);
            }
            variantEvaluation.setFrequencyData(AlleleProtoAdaptor.toFrequencyData(alleleProperties));
            variantEvaluation.setPathogenicityData(AlleleProtoAdaptor.toPathogenicityData(alleleProperties));
            return variantEvaluation;
        };
    }

    private static class StubVariantAnnotator implements VariantAnnotator {

        private static final VariantAnnotation EMPTY_ANNOTATION = VariantAnnotation.of(".", "", VariantEffect.SEQUENCE_VARIANT, List.of());

        @Override
        public GenomeAssembly genomeAssembly() {
            return GenomeAssembly.HG19;
        }

        @Override
        public List<VariantAnnotation> annotate(Variant variant) {
            return List.of(EMPTY_ANNOTATION);
        }
    }

    private JannovarData loadJannovarData() {
        Path transcriptFilePath = Paths.get("/home/hhx640/Documents/exomiser-data/1902_hg19/1902_hg19_transcripts_ensembl.ser");
        return JannovarDataSourceLoader.loadJannovarData(transcriptFilePath);
    }

    private static class StubAllelePropertiesDao implements AllelePropertiesDao {
        @Override
        public AlleleProto.AlleleProperties getAlleleProperties(AlleleProto.AlleleKey alleleKey, GenomeAssembly genomeAssembly) {
            return AlleleProto.AlleleProperties.getDefaultInstance();
        }

        @Override
        public AlleleProto.AlleleProperties getAlleleProperties(org.monarchinitiative.exomiser.core.model.Variant variant) {
            return AlleleProto.AlleleProperties.getDefaultInstance();
        }
    }

    private static AllelePropertiesDao allelePropertiesDao() {
        Path mvStorePath = Paths.get("/home/hhx640/Documents/exomiser-data/1811_hg19/1811_hg19_variants.mv.db")
                .toAbsolutePath();
        MVStore mvStore = new MVStore.Builder()
                .fileName(mvStorePath.toString())
                .cacheSize(32)
                .readOnly()
                .open();
        return new AllelePropertiesDaoMvStore(mvStore);
    }
}
