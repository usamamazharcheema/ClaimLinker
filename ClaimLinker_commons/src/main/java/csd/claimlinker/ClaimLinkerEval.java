package csd.claimlinker;

import csd.claimlinker.model.Association_type;
import csd.claimlinker.model.CLAnnotation;
import csd.claimlinker.model.Claim;
import csd.claimlinker.nlp.AnalyzerDispatcher;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClaimLinkerEval {

    private static HashMap<String, String> tweetID2text = new HashMap<>();
    private static HashMap<String, String> tweetID2textlabels = new HashMap<>();
    private static HashMap<String, String> tweetID2claimID = new HashMap<>();
    private static HashMap<String, String> claimID2claimText = new HashMap<>();
    private static HashMap<String, String> claimID2claimTitle = new HashMap<>();
    static double overall_timeElapsed_tweet = 0;
    
    public static FileWriter writer = null;
    public static FileWriter runfile_writer = null;
   
    public static List<List<String>> parent = new ArrayList<>();
    
    public static void readGTdata() {
    	Instant start_tweet = Instant.now();
        String filepath = "data/tweets.queries.tsv";
        try (FileInputStream fis = new FileInputStream(filepath);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr)) {

            String str;
            int n = 0;
            while ((str = reader.readLine()) != null) {
                n++;
                if (n == 1) {
                    continue;
                }
                String data[] = str.split("\t");
                String tweetId = data[0];
                String tweetContent = data[1];
                String tweetLabels = data[2];
                tweetID2text.put(tweetId, tweetContent);
                tweetID2textlabels.put(tweetId, tweetLabels);

//                System.out.println(tweetId);
//                System.out.println(tweetContent);
//                System.out.println("-------");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        Instant finish_tweet = Instant.now();
        long timeElapsed_tweet = Duration.between(start_tweet, finish_tweet).toMillis();
        overall_timeElapsed_tweet = (double) timeElapsed_tweet / 1000 + overall_timeElapsed_tweet;
        System.out.println("Testing Tweet time");
        System.out.println(overall_timeElapsed_tweet);
        System.out.println("Tweets Size: " + tweetID2text.size());

        System.out.println("================");

        String filepath2 = "data/tweet-vclaim-pairs.qrels";
        try (FileInputStream fis = new FileInputStream(filepath2);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr)) {

            String str;
            while ((str = reader.readLine()) != null) {
                String data[] = str.split("\t");
                String tweetId = data[0];
                String claimId = data[2];

                if (tweetID2claimID.containsKey(tweetId)) {
                    System.out.println("SOS SOS " + tweetId);
                }
                tweetID2claimID.put(tweetId, claimId);

                System.out.println(tweetId);
                System.out.println(claimId);
                System.out.println("-------");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("GT size: " + tweetID2claimID.size());

        System.out.println("================");

        String filepath3 = "data/verified_claims.docs.tsv";
        try (FileInputStream fis = new FileInputStream(filepath3);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr)) {

            String str;
            while ((str = reader.readLine()) != null) {
                String data[] = str.split("\t");
                String num = data[0];
                String claimText = data[1];
                String claimTitle = data[2];

                claimID2claimText.put(num, claimText);
                claimID2claimTitle.put(num, claimTitle);

                System.out.println(num);
                System.out.println(claimText);
                System.out.println(claimTitle);
                System.out.println("-------");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Claim num 2 text size: " + claimID2claimText.size());
        System.out.println("Claim num 2 title size: " + claimID2claimTitle.size());

    }

    public static void runExperiments() throws IOException, ClassNotFoundException {
        readGTdata();

        int tweetNumber = 0;
        int annotTotalNumber = 0;
        int correctTop1 = 0;
        int correctTop2 = 0;
        int correctTop3 = 0;
        int correctTop4 = 0;
        int correctTop5 = 0;
        int linkedclaimfalse = 0;
        int testingfirstTrueClaims = 0;
        int correctInElasticList= 0;
        double pAt3 = 0;
        double pAt5 = 0;
        double mrrAt= 0;        
        
        double corIn20= 0;
        double corInOverall= 0;
        
        double avgPreAt1 =0;
        double avgPreAt3 =0;
        double avgPreAt5 =0;
        double avgPreAt20 =0;
        double avgPreAt30 =0;
        double overall_timeElapsed = 0;
        
        double rrAt1= 0;
        double rrAt2= 0;
        double rrAt5= 0;
        writer = new FileWriter("test.csv");
        runfile_writer = new FileWriter("run_type.csv");
        for (String tweetID : tweetID2text.keySet()) {
        	Instant start_tw = Instant.now();
            tweetNumber++;
            String tweetText = tweetID2text.get(tweetID);
            String tweetTextlabels = tweetID2textlabels.get(tweetID);
            
//            int pos1 = tweetText.toLowerCase().indexOf("http");
//            
//            if (pos1 != -1) {
//                int pos2 = tweetText.indexOf(" ", pos1 + 1);
//                
//            String substr = tweetText.substring(pos1, pos2);
//            tweetText = tweetText.replace(substr, " ");
//            }
//
//            int pos3 = tweetText.toLowerCase().indexOf("pic.twitter");
//            if (pos3 != -1) {
//            int pos4 = tweetText.indexOf(" ", pos3 + 1);
//            String substr = tweetText.substring(pos3, pos4);
//            tweetText = tweetText.replace(substr, " ");
//            }
//
//            int pos5 = tweetText.lastIndexOf("— ");
//            if (pos5 != -1) {
//            tweetText = tweetText.substring(0, pos5);
//            }
//
//            tweetText = tweetText.replace(".", " ").replace("!", " ").replace(";", " ").replace("?", " ");
//            while (tweetText.contains("  ")) {
//            tweetText = tweetText.replace("  ", " ");
//            }
//            
//            String tweetText2 = "";
//            if(tweetText.contains("#")) {
//            	
//            String[] stringParts = tweetText.split("#");
//            for(String part : stringParts)
//            {
//            tweetText2 = tweetText2 +part; 
//            }
////            	System.out.println("---------Testing----------");
////                System.out.println(tweetText2);
//            }
//            else {
////            tweetText2 = tweetText; 
//            }
//            
            try {
                Set<CLAnnotation> annots = demo_pipeline(tweetText, tweetTextlabels);
                Instant finish_tw = Instant.now();
                long timeElapsed_tw = Duration.between(start_tw, finish_tw).toMillis();
                overall_timeElapsed = (double) timeElapsed_tw / 1000 + overall_timeElapsed;
                System.out.println("Testing Overall list");
                System.out.println(overall_timeElapsed);
//                System.out.println(overall_timeElapsed);
                int annotNumber = 1;
//                if (tweetNumber == 5) {
//                    break;
//                }

                //System.out.println("Tweet_" + tweetNumber + " = " + tweetID + " (" + tweetText + ")");
                if (!annots.isEmpty()) {
                    annotTotalNumber++;
                }
             
                int correctIn3 = 0;
                int correctIn5 = 0;
                int correctIn20 = 0;
                int correctInOverall = 0;
                
                                                               
                double sum_precisionAt1 =0;
                double avg_precisionAt1=0;
                double sum_precisionAt3 =0;
                double avg_precisionAt3=0;
                double sum_precisionAt5 =0;
                double avg_precisionAt5=0;
                double sum_precisionAt20 =0;
                double avg_precisionAt20=0;
                double sum_precisionAt30 =0;
                double avg_precisionAt30=0;
                
                double mrrIn=0;
                double rrIn2=0;
                double rrIn5=0;
                for (CLAnnotation annot : annots) {

                    System.out.println("  Annot_" + (annotNumber++) + " (" + annot.getText() + ")");
                    List<Claim> linkedClaims = annot.getLinkedClaims();
                    int linkedClaimsNumber = 0;
                    Collections.sort(linkedClaims, Collections.reverseOrder());
                    
                    if (linkedClaims != null) {
                    	for (Claim linkedClaim : linkedClaims) {
                    		List<String> test = new ArrayList<>();
                            linkedClaimsNumber++;
                            System.out.println("Tweet_" + tweetNumber + " = " + tweetID + " (" + tweetText + ")");
                            double nlpScore = (linkedClaim.getNLPScore() * 100.0);
                            double ESScore = linkedClaim.getElasticScore();
                            double sum = nlpScore + ESScore;
                            
                            String claimID = linkedClaim.getclaimReviewedURL();
    
                            boolean rel = false;
                            String correctClaimID = tweetID2claimID.get(tweetID);
                            String correctClaimText= claimID2claimText.get(correctClaimID);
                            
                            System.out.println("Testing data");
                            System.out.println(tweetID + " " + "Q0" + " " + claimID + " " + "1" + " " + sum/100.0 + " " + "elastic_search" + " " + correctClaimID);
                            
//                            runfile_writer.append(tweetID);
//                            runfile_writer.append(",");
//                            runfile_writer.append("Q0");
//                            runfile_writer.append(",");
//                            runfile_writer.append(claimID);
//                            runfile_writer.append(",");
//                            runfile_writer.append("1");
//                            runfile_writer.append(",");
//                            runfile_writer.append(String.valueOf(sum/100.0));
//                            runfile_writer.append(",");
//                            runfile_writer.append("elastic_search");
//                            runfile_writer.append("\n");
//                           
                            test.add(tweetID);
                            test.add("Q0");
                            test.add(claimID);
                            test.add("1");
                            test.add(String.valueOf(sum/100.0));
                            test.add("elastic_search");
                            String collect = test.stream().collect(Collectors.joining(","));
                            runfile_writer.write(collect);
                            runfile_writer.write("\n");
                            System.out.println(collect);
                            
                            
                            if (correctClaimID.equals(claimID)) {
                                rel = true;
                                System.out.println("-------------Corectly classified claims-----------");
                                System.out.println("correct claim ID" + correctClaimID + " linked claim ID " + claimID);
                                System.out.println(correctClaimText);
                            }

                            
                            
                            System.out.println("LinkedClaim_" + linkedClaimsNumber + ": " + linkedClaim.getReviewedBody() + "[" + claimID + "] [" + sum + "] [" + rel + "]");
                            
                            if (linkedClaimsNumber <=2 && rel) {
                            	rrIn2= 1.0/linkedClaimsNumber;
//                            	System.out.println("---------Testing_rrIn2----------");
//                            	System.out.println(linkedClaimsNumber + " " +rrIn2);
                            	
                                
                            }
                            if (linkedClaimsNumber <=3 && rel) {
                                correctIn3++;
                              //Average Precision at 3
                                sum_precisionAt3 =  (1.0 /(linkedClaimsNumber));
                                avg_precisionAt3= sum_precisionAt3 / Math.min(5.0, 1.0);
//                                System.out.println("-----Testing Averagat3-----");
//                                System.out.println(linkedClaimsNumber);
                                
                                
                            }
                           
                            if (linkedClaimsNumber <=5 && rel) {
                                correctIn5++;
                                //Average Precision at 5
                                sum_precisionAt5 = (1.0 /( linkedClaimsNumber));
                                avg_precisionAt5= sum_precisionAt5 / Math.min(5.0, 1.0);
//                                System.out.println("-----Testing Averagat5-----");
//                                System.out.println(linkedClaimsNumber);
//                                System.out.println(sum_precisionAt5);
//                                System.out.println(avg_precisionAt5);
//                                
                                
                                rrIn5= 1.0/ linkedClaimsNumber;  //Reciprocal Rank At 5
                                
                               
                                
                            }
                            
                            if (linkedClaimsNumber <=10 && rel) {
//                            	System.out.println("-----Testing Averagat5-----");
//                            	System.out.println(linkedClaimsNumber);                                
                                
                                mrrIn= 1.0/( linkedClaimsNumber);  //MRR
                                System.out.println(mrrIn);
                                mrrAt +=mrrIn;
                                
                            }
                            
                            if (linkedClaimsNumber <=20 && rel) {
                                //Average Precision at 20
                                sum_precisionAt20 = (1.0 /( linkedClaimsNumber));
                                avg_precisionAt20= sum_precisionAt20 / Math.min(5.0, 1.0);
//                                System.out.println("-----Testing Averagat5-----");
//                                System.out.println(linkedClaimsNumber);
//                                System.out.println(sum_precisionAt20);
//                                System.out.println(avg_precisionAt20);
                                correctIn20++;
                                
                                
                            }
                            
                            if(rel){
                            	correctInOverall++;
                            }
                            
                            if (linkedClaimsNumber <=30 && rel) {
                                //Average Precision at 30
                                sum_precisionAt30 = (1.0 /( linkedClaimsNumber));
                                avg_precisionAt30= sum_precisionAt30 / Math.min(5.0, 1.0);
//                                System.out.println("-----Testing Averagat5-----");
//                                System.out.println(linkedClaimsNumber);
//                                System.out.println(sum_precisionAt30);
//                                System.out.println(avg_precisionAt30);
                                
                            }
                            
                            
                            
                            if (linkedClaimsNumber == 1 && rel) {
                            	
                            	double rrIn1= 1.0 /linkedClaimsNumber;
                            	rrAt1 +=rrIn1;
                            	sum_precisionAt1 =  (1.0 /(linkedClaimsNumber));
                                avg_precisionAt1= sum_precisionAt1 / Math.min(5.0, 1.0);
                                testingfirstTrueClaims += 1;
                                System.out.println("-----Testing true claims-----");
                                System.out.println(testingfirstTrueClaims);
                                System.out.println(correctTop1);
                                System.out.println(correctTop2);
                                
                                
                                
                                correctTop1++;
                                correctTop2++;
                                correctTop3++;
                                correctTop4++;
                                correctTop5++;
                                break;
                            }
                            if(linkedClaimsNumber == 1) {
                            	for (Claim testingLinkedClaim : linkedClaims) {
                            		String testingClaimID = testingLinkedClaim.getclaimReviewedURL();
                            		if (correctClaimID.equals(testingClaimID)) {
                                		correctInElasticList++;
                                		System.out.println("Correct claim does exist in the FULL list of retrieved claims" + correctInElasticList + rel);
                                    }
                            		
                            	}
                            	
                            	linkedclaimfalse++;
//                            	System.out.println("wrongly classified claims" + linkedclaimfalse);
//                            	System.out.println(correctClaimID + " "+correctClaimText);
                            	System.out.println("    LinkedClaimfalse_" + linkedClaimsNumber + ": " + linkedClaim.getReviewedBody() + "[" + claimID + "] [" + sum + "] [" + rel + "]");

                            }
                            if (linkedClaimsNumber == 2 && rel) {
                                correctTop2++;
                                correctTop3++;
                                correctTop4++;
                                correctTop5++;
                                break;
                            }
                            if (linkedClaimsNumber == 3 && rel) {
                                correctTop3++;
                                correctTop4++;
                                correctTop5++;
                                break;
                            }
                            if (linkedClaimsNumber == 4 && rel) {
                                correctTop4++;
                                correctTop5++;
                                break;
                            }
                            if (linkedClaimsNumber == 5 && rel) {
                                correctTop5++;
                                break;
                            }
                            
                            

                        }
                    }
                    
                    double corIn3 = (double)correctIn3 / 3.0;
                    pAt3 += corIn3;
//                    System.out.println("---------Testing----------");
//                    System.out.println((double)correctIn3 + " "+corIn3 + " " +pAt3);
                    double corIn5 = (double) correctIn5 / 5.0;
                    pAt5 += corIn5;
//                    System.out.println((double)correctIn5 + " "+corIn5 + " " +pAt5);
                    System.out.println("---------Testing----------");
                    corIn20 += correctIn20;
//                    System.out.println(corIn20);
//                    System.out.println(corIn20);
                    corInOverall += correctInOverall;
                    
                    avgPreAt1 += avg_precisionAt1;
                    avgPreAt3 += avg_precisionAt3;
                    avgPreAt5 += avg_precisionAt5;
                    avgPreAt20 += avg_precisionAt20;
                    avgPreAt30 += avg_precisionAt30;
//                    System.out.println(avgPreAt30);
//                    mrrAt +=mrrIn;
//                    System.out.println(mrrAt);
//                    rrIn2= rrIn2 / 2;
                	rrAt2 +=rrIn2;
                	rrAt5 +=rrIn5;
//
//                	rrIn5= rrIn5 / 5;  //Reciprocal Rank At 5
                	
                    }
                    	
                    }
                    	
                                    
                 catch (ClassNotFoundException ex) {
                Logger.getLogger(ClaimLinkerEval.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        try {
        	runfile_writer.flush();
        	runfile_writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        double avgPat3 = pAt3 / (double) tweetNumber;
        double avgPat5 = pAt5 / (double) tweetNumber;
//        System.out.println("---------Testing----------");
//        System.out.println(pAt3+ " " +tweetNumber + " " + avgPat3);  
//        System.out.println(pAt5+ " " +tweetNumber + " " + avgPat5);
        System.out.println("TOTAL NUMBER OF TWEETS (TEST CASES) = " + tweetNumber);
        System.out.println("TOTAL NUMBER OF TWEET ANNOTATIONS = " + annotTotalNumber);
        System.out.println("TOTAL NUMBER OF CORRECT TOP-1 ANNOTATIONS = " + correctTop1);
        System.out.println("TOTAL NUMBER OF CORRECT TOP-2 ANNOTATIONS = " + correctTop2);
        System.out.println("TOTAL NUMBER OF CORRECT TOP-3 ANNOTATIONS = " + correctTop3);
        System.out.println("TOTAL NUMBER OF CORRECT TOP-4 ANNOTATIONS = " + correctTop4);
        System.out.println("TOTAL NUMBER OF CORRECT TOP-5 ANNOTATIONS = " + correctTop5);
        System.out.println("P@1 = " + (double) correctTop1 / (double) tweetNumber );
        System.out.println("P@3 = " + avgPat3);
        System.out.println("P@5 = " + avgPat5);
        
//        System.out.println("Testing AvgPreAt1= " + avgPreAt1);
//        System.out.println("Tweet No = " + tweetNumber);
        System.out.println("MAPat1 = " + avgPreAt1 / (double) tweetNumber );
        System.out.println("MAPat3 = " + avgPreAt3 / (double) tweetNumber );
        System.out.println("MAPat5 = " + avgPreAt5 / (double) tweetNumber );
        System.out.println("MAPat20 = " + avgPreAt20 / (double) tweetNumber );
//        System.out.println("MAPat30 = " + avgPreAt30 / (double) tweetNumber );
        
      //Making changes for reciprocal rank matrix
        System.out.println("MRR = " + mrrAt / (double) tweetNumber );
        
//        System.out.println(tweetNumber);
        System.out.println("RR@1 = " + rrAt1 / (double) tweetNumber);
        System.out.println("RR@2 = " + rrAt2 / (double) tweetNumber); //67.5/200
        System.out.println("RR@5 = " + rrAt5 / (double) tweetNumber);
        
        System.out.println("Total Relavant Claims in 20 = " + corIn20 + " " +(double) corIn20 / (double) tweetNumber );
//        System.out.println("Overall Relavant Claims = " + corInOverall  + " " +(double) corInOverall / (double) tweetNumber );
    }

    public static void main(String[] args) throws Exception {
        //testClaimLink();
        runExperiments();
    }

    static void testClaimLink() throws Exception {
        System.out.println("[Demo]Initiating ...");
        System.out.println("_______________________________________________");
        //demo_pipeline("Of course, we are 5 percent of the world's population; we have to trade with the other 95 percent.\n");
//        demo_pipeline("Mitch Daniels says interest on debt will soon exceed security spending.\n");
        System.out.println("_______________________________________________");
        System.out.println("[Demo]Exited without errors ...");

    }

    public static Set<CLAnnotation> demo_pipeline(String text, String tweetTextlabels) throws IOException, ClassNotFoundException {
        AnalyzerDispatcher.SimilarityMeasure[] similarityMeasures = new AnalyzerDispatcher.SimilarityMeasure[]{
            AnalyzerDispatcher.SimilarityMeasure.jcrd_comm_words, //Common (jaccard) words
            AnalyzerDispatcher.SimilarityMeasure.jcrd_comm_lemm_words, //Common (jaccard) lemmatized words
//            AnalyzerDispatcher.SimilarityMeasure.jcrd_comm_ne, //Common (jaccard) named entities
            AnalyzerDispatcher.SimilarityMeasure.jcrd_comm_dissambig_ents, //Common (jaccard) disambiguated entities BFY
            AnalyzerDispatcher.SimilarityMeasure.jcrd_comm_pos_words, //Common (jaccard) words of specific POS
            AnalyzerDispatcher.SimilarityMeasure.jcrd_comm_srl_words, //Common (jaccard) words of specific POS

            AnalyzerDispatcher.SimilarityMeasure.jcrd_comm_ngram, //Common (jaccard) ngrams
            AnalyzerDispatcher.SimilarityMeasure.jcrd_comm_nchargram, //Common (jaccard) nchargrams
            AnalyzerDispatcher.SimilarityMeasure.vec_cosine_sim //Cosine similarity
        };
        
        ClaimLinker CLInstance = new ClaimLinker(5, similarityMeasures, "data/stopwords.txt", "data/puncs.txt", "data/english-20200420.hash", "localhost");
        System.out.println("Demo pipeline started!");
        Set<CLAnnotation> results = CLInstance.claimLink(text, tweetTextlabels, 5, Association_type.same_as);
        return results;
    }

}