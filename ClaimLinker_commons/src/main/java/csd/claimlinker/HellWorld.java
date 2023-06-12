package csd.claimlinker;
import csd.claimlinker.nlp.*;

import java.util.*;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.*;
import edu.stanford.nlp.util.*;

public class HellWorld {
    public static void main(String[] args) {
        // create a new pipeline with the semantic role labeling annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment, kbp, ner, entitymentions, coref, natlog, openie, depparse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // define a complex sentence with multiple verbs
        String sentence = "The cat chased the mouse and then jumped over the fence.";

        // create an annotation object for the sentence
        Annotation document = new Annotation(sentence);

        // annotate the document using the pipeline
        pipeline.annotate(document);

        // extract the semantic role labels for the sentence
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sent : sentences) {
            SemanticGraph graph = sent.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
            List<SemanticGraphEdge> edges = graph.edgeListSorted();
            for (SemanticGraphEdge edge : edges) {
                if (edge.getRelation().getShortName().equals("ARG0") || 
                    edge.getRelation().getShortName().equals("ARG1") || 
                    edge.getRelation().getShortName().equals("ARG2") || 
                    edge.getRelation().getShortName().equals("ARG3") || 
                    edge.getRelation().getShortName().equals("ARG4") || 
                    edge.getRelation().getShortName().equals("ARG5")) {
                    IndexedWord arg = edge.getDependent();
                    IndexedWord verb = edge.getGovernor();
                    System.out.println(edge.getRelation().getShortName() + "(" + verb.word() + ", " + arg.word() + ")");
                }
            }
        }
    }
}
