package task;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static String integers   =   "integers.txt";
    public static String floats     =   "floats.txt";
    public static String strings    =   "strings.txt";

    public static String extraPath  =   "";
    public static String prefix     =   "";

    public static boolean statS     =   false;
    public static boolean statF     =   false;

    public static  ArrayList<String> fileNames = new ArrayList<>();
    public static Path runPath;

    public static boolean adding    =   false;
    public static boolean alreadyOpenedString =   false;
    public static boolean alreadyOpenedFloat  =   false;
    public static boolean alreadyOpenedInt    =   false;

    public static long numbLong     =   0;
    public static long numbDouble   =   0;
    public static long numbStr      =   0;

    public static volatile ArrayList<Long>   listLong   =   new ArrayList<>();
    public static volatile ArrayList<Double> listDouble =   new ArrayList<>();
    public static volatile ArrayList<String> listString =   new ArrayList<>();


    public static void main(String[] args) {

        try{
            runPath = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        options(args);

        ExecutorService executor = Executors.newFixedThreadPool(fileNames.size());
        CountDownLatch latch = new CountDownLatch(fileNames.size());

        for (String fileName : fileNames) {
            executor.submit(() -> {
                takingData(runPath.toString() + "\\"+ fileName);
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        statistic();

        listWriter(extraPath+prefix+strings,alreadyOpenedString,listString);
        listWriter(extraPath+prefix+floats,alreadyOpenedFloat,listDouble);
        listWriter(extraPath+prefix+integers,alreadyOpenedInt,listLong);
    }

    public static void options(String[] args){

        try{
            for (int i=0; i<args.length; i++) {
                String str = args[i];
                if(str.charAt(0)=='-'){
                    switch (str){
                        case "-a":
                            adding = true;
                            break;
                        case "-o":
                            if(args[i+1].charAt(0)=='-')
                                throw new ConsoleInputExeption("Input exeption");

                            extraPath=args[i+1]+"\\";

                            if(!Paths.get(extraPath).toFile().exists())
                                throw new ConsoleInputExeption("Input directory does not exist");

                            i++;
                            break;

                        case "-p":
                            if(args[i+1].charAt(0)=='-')
                                throw new ConsoleInputExeption("Input exeption");

                            prefix=args[i+1];
                            i++;
                            break;
                        case "-s":
                            statS = true;
                            break;
                        case "-f":
                            statS = true;
                            statF = true;
                            break;
                        default:
                            throw new ConsoleInputExeption("Unknown option: "+str);
                    }
                }
                else{
                    if(!str.endsWith(".txt"))
                        throw new ConsoleInputExeption("Incorrect file type: "+str);
                    fileNames.add(str);
                }
            }
            if(!fileNames.isEmpty()){
                for(int i=0; i<fileNames.size(); i++){
                    //System.out.println(extraPath+prefix+fileNames);
                    File file = new File(runPath.toString()+"\\"+ fileNames.get(i));
                    //System.out.println(file.exists());
                    if(!file.exists())
                        throw new ConsoleInputExeption("File does not exist: "+file);
                    if(!file.canRead())
                        throw new ConsoleInputExeption("File not for reading: "+file);

                }
            }

            fileCheck(new File(extraPath+prefix+strings));
            fileCheck(new File(extraPath+prefix+integers));
            fileCheck(new File(extraPath+prefix+floats));

        }
        catch (ConsoleInputExeption e){
            System.out.println(e.getMessage());
            System.exit(1);
        }

    }

    public static void fileCheck(File file) throws ConsoleInputExeption{
        String fileName = file.getName();
        if(!file.exists() && adding){
            throw new ConsoleInputExeption("File for adding does not exist: "+ fileName);
        }
        if(file.exists() && !file.canWrite()){
            throw new ConsoleInputExeption("File only for reading: "+ fileName);
        }
    }

    public static void takingData(String inputFileName){

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineDistribution(line);
            }
        }
        catch (IOException e) {
            System.err.println("Reading file Error " + inputFileName + ": " + e.getMessage());
        }
    }

    public static synchronized void lineDistribution(String line){

        try{
            listLong.add(Long.valueOf(line.trim()));
        }
        catch (NumberFormatException numberFormatExceptionLong){
            try{
                listDouble.add(Double.valueOf(line.trim()));

            }catch (NumberFormatException numberFormatExceptionDouble){
                if(!line.replaceAll("\\s","").isEmpty())
                    listString.add(line.trim());
            }
        }
    }

    public static boolean lineWriter(String outputFileName,  boolean alreadyOpened, String line){

        boolean formatWrite = true;

        if(alreadyOpened == false){
            formatWrite = adding;
            alreadyOpened = true;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName, formatWrite))) {
            writer.write(line+"\n");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return alreadyOpened;
    }

    public static void listWriter(String outputFileName,boolean alreadyOpened, ArrayList list){
        for(Object elem : list) {
            alreadyOpened = lineWriter(outputFileName, alreadyOpened,elem.toString());
        }
    }

    public static void statistic(){

        long sumLong =   0;
        double sumDouble =   0;

        numbLong = listLong.stream().count();
        numbDouble = listDouble.stream().count();
        numbStr = listString.stream().count();

        if(statS)
            System.out.println("\nnumber of written elements = "+(numbLong+ numbDouble +numbStr));
        if(statF){
            //System.out.println("Stats:");
            if(!listLong.isEmpty())
                System.out.println("\nIntegers:"+
                        "\nnumber  = " + numbLong +
                        "\nmax     = " + Collections.max(listLong) +
                        "\nmin     = " + Collections.min(listLong) +
                        "\nsum     = " + (sumLong = listLong.stream().mapToLong(i->i).sum()) +
                        "\naverage = " + sumLong /numbLong );
            if(!listDouble.isEmpty())
                System.out.println("\nFloats:"+
                        "\nnumber  = " + numbDouble +
                        "\nmax     = " + Collections.max(listDouble) +
                        "\nmin     = " + Collections.min(listDouble) +
                        "\nsum     = " + (sumDouble = listDouble.stream().mapToDouble(i->i).sum()) +
                        "\naverage = " + sumDouble / numbDouble);
            if(!listString.isEmpty()){
                System.out.println("\nString:"+
                        "\nnumber          = " + numbStr +
                        "\nmax size string = " + listString.stream().mapToLong(String::length).max().getAsLong() +
                        "\nmin size string = " + listString.stream().mapToLong(String::length).min().getAsLong()
                );
            }
        }
    }

}