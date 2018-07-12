package smartRegex;


import org.apache.commons.cli.*;
import regex.operators.AllMutators;
import smartRegex.evolutionEngine.*;
import smartRegex.utils.LabeledString;
import smartRegex.utils.RegexCandidate;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainClass {

    private static ExecutionMode mode = ExecutionMode.MULTI_THREAD_V2;
    public static boolean useFile = false;
    public static float HOM_PERC = 0.2f;
    public static boolean USE_HOM = true;
    public static int N_POP = 50;
    public static int N_ITER = 20;
    public static int N_PARENTS = 25;
    public static int N_STRINGS = 1000;
    public final static int N_HOM_THREADS = 100;

    public static boolean SPECIALIZE = true;

    public static int MAX_INFINITE = 5;

    public static String REGEX_ORACLE = "[A-Z]{2}[0-9]{3}[A-Z]{2}";
    public static String REGEX_UNIVERSE = "[a-zA-Z0-9]{7}";
    public static String REGEX_START = "[a-z]+";

    public static RegexCandidate finalRegex;
    public static double finalFri;

    public static List<LabeledString> strings = new ArrayList<>();

    private enum ExecutionMode {
        MONO_THREAD,
        MULTI_THREAD,
        MULTI_THREAD_V2,
        MULTI_THREAD_HYPER_SCAN
    }

    public static void main(String[] args) {

        cmdParser(args);

        AllMutators.enableOnly(new String[]{"CC", "RMN", "CCA", "QC", "PA", "CCR"});
        EvolutionEngine engine;
        switch (mode) {
            case MONO_THREAD:
                engine = new MonoThreadEngine();
                break;
            case MULTI_THREAD:
                engine = new MultiThreadEngine();
                break;
            case MULTI_THREAD_V2:
                engine = new MultiThreadV2Engine();
                break;
            case MULTI_THREAD_HYPER_SCAN:
                engine = new MultiHyperScanEngine();
                break;
            default:
                throw new RuntimeException();
        }


        double[] profData = engine.run();

        System.out.println("\n\n\n~*~*~*~*~*~*~*~*~" + mode.toString() + "~*~*~*~*~*~*~*~*~\n");
        System.out.println("   --- RESULTS ---   \n");
        System.out.println("BEST REGEX FOUND: " + finalRegex.regex.toString() +
                "\n                  whit fitness " + finalRegex.fitness +
                "\n                  and failure residual index " + finalFri);
        System.out.println("TOTAL TIME: " + (int) profData[0] + "ms");
        System.out.println("N. OFFSPRING : " + (int) profData[1]);
        System.out.println("MILLIS/OFFSPRING: " + profData[0] / profData[1]);


    }

    private static void cmdParser(String[] args){
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        Option r_oracolo = Option.builder("rO")
                .argName("regex")
                .hasArg()
                .desc(  "the regex the program has to evolve into" )
                .build();
        Option r_universo = Option.builder("rU")
                .argName("regex")
                .hasArg()
                .desc(  "the regex used to generate wrong string for the oracle" )
                .build();
        Option r_partenza = Option.builder("rS")
                .argName("regex")
                .hasArg()
                .desc(  "the starting regex" )
                .build();
        Option inputFile = Option.builder("f")
                .argName("file")
                .hasArg()
                .desc(  "the program will find the regex that match the strings in this file [default: generate a set of strings from rO and rU]" )
                .build();
        Option npop = Option.builder("npop")
                .argName("int")
                .hasArg()
                .desc(  "the size of the population [default 50]" )
                .build();
        Option niter = Option.builder("ngen")
                .argName("int")
                .hasArg()
                .desc(  "the number of generations to calculate [default 20]" )
                .build();
        Option npar = Option.builder("npar")
                .argName("int")
                .hasArg()
                .desc(  "how many regexes from the population that will become parents for the new generation [default 25]" )
                .build();
        Option nstrings = Option.builder("nstrings")
                .argName("int")
                .hasArg()
                .desc(  "how many strings has to be generated [default 1000]" )
                .build();
        Option nstar = Option.builder("ninf")
                .argName("int")
                .hasArg()
                .desc(  "the max value of repetition that can be taken to replace the infinite loop created by the regex operators *, +, {1,}, ... [default 5]" )
                .build();
        Option homperc = Option.builder("homperc")
                .argName("float [0,1]")
                .hasArg()
                .desc(  "the percentage of the parents that can receive higher order mutation (HOM) in one generation [default 0.2]" )
                .build();
        options.addOption(r_oracolo)
                .addOption(r_universo)
                .addOption(r_partenza)
                .addOption(inputFile)
                .addOption(npop)
                .addOption(npar)
                .addOption(niter)
                .addOption(nstrings)
                .addOption(nstar)
                .addOption(homperc);
        options.addOption("mono", false, "use mono thread evolution");
        options.addOption("multi", false, "use multi thread evolution");
        options.addOption("multi2", false, "use enhanced multi thread evolution [default]");
        options.addOption("hyper", false, "use Hyperscan evolution (only Linux)");
        options.addOption("h", false, "display this help message");
        options.addOption("nospecialize", false, "the final regex is not specialized");
        options.addOption("disablehom", false, "disable HOM mutation");
        HelpFormatter formatter = new HelpFormatter();

        String header = "\nThis program try to find the regex that match more a set of given (or generated) strings using evolutionary computing\n" +
                "Every process variable can be set\n\nOptions:";
        String footer = "\nThe default values are a demo of the program. This has to find the regex for Italian plates [A-Z]{2}[0-9]{3}[A-Z]{2} starting from [a-z]+";

        try {
            CommandLine line = parser.parse(options, args);
            if(line.hasOption("h")){
                formatter.printHelp( "SmartRegex",header, options, footer, true );
                System.exit(0);
            }
            int nModes = 0;
            if(line.hasOption("mono")){
                mode = ExecutionMode.MONO_THREAD;
                nModes++;
            }
            if(line.hasOption("multi")){
                mode = ExecutionMode.MULTI_THREAD;
                nModes++;
            }
            if(line.hasOption("multiV2")){
                mode = ExecutionMode.MULTI_THREAD_V2;
                nModes++;
            }
            if(line.hasOption("hyper")){
                mode = ExecutionMode.MULTI_THREAD_HYPER_SCAN;
                nModes++;
            }
            if(nModes > 1){
                System.out.println("Error: you have to specify only one execution mode!\n");
                System.exit(1);
            }
            String filePath = line.getOptionValue("f");
            if (filePath != null){
                useFile = true;
                fileInParser(filePath);
            } else{
              String rO = line.getOptionValue("rO");
              String rU = line.getOptionValue("rU");
              if(rO != null) REGEX_ORACLE = rO;
              if(rU != null) REGEX_UNIVERSE = rU;
            }

            String rS = line.getOptionValue("rS");
            if(rS != null) REGEX_START = rS;

            String nPop = line.getOptionValue("npop");
            String nIter = line.getOptionValue("ngen");
            String nPar = line.getOptionValue("npar");
            String nStrings = line.getOptionValue("nstrings");
            String nStar = line.getOptionValue("nstar");
            String homPerc = line.getOptionValue("homperc");
            if(nPop != null) N_POP = Integer.parseInt(nPop);
            if(nIter != null) N_ITER = Integer.parseInt(nIter);
            if(nPar != null) N_PARENTS = Integer.parseInt(nPar);
            if(nStrings != null) N_STRINGS = Integer.parseInt(nStrings);
            if(nStar != null) MAX_INFINITE = Integer.parseInt(nStar);
            if(homPerc != null) HOM_PERC = Float.parseFloat(homPerc);

            if(line.hasOption("nospecialize"))    SPECIALIZE = false;
            if(line.hasOption("disablehom"))    USE_HOM = false;


        } catch (ParseException e) {
            header = "\nError: check the syntax!\n\nOptions:";
            formatter.printHelp( "SmartRegex", header, options, null, true );
            System.exit(1);
        }
    }

    private static void fileInParser(String filePath){
        try {
            FileReader frd = new FileReader(filePath);
            Scanner s = new Scanner(frd);
            int currentLine = 1;
            while (s.hasNext()){
                String line = s.nextLine();
                String[] lsplit = line.split(" ");
                if (lsplit.length != 2 || !(lsplit[1].equals("C") || lsplit[1].equals("W")))
                    throw new Exception("" + currentLine);
                boolean correct = lsplit[1].equals("C");
                strings.add(new LabeledString(lsplit[0], correct));
                currentLine++;
            }

        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
            System.exit(1);
        } catch (Exception l){
            System.out.println("Error reading the file at line " + l.getMessage());
            System.exit(1);
        }
    }
}
