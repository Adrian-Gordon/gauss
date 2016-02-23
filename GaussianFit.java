/*
*   Application:   GaussianFit
*
*   USAGE:  An application for fitting examination marks to a Gaussian distribution
*           This application illustrates the class, Regression.
*
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:       December 2010
*   UPDATE:     1 February 2015
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web pages:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/GaussianFit.html
*   http://www.ee.ucl.ac.uk/~mflanaga/java/Regression.html
*
*   Copyright (c) 2010 - 2015
*
*   PERMISSION TO USE:
*
*   Redistributions of this source code, or parts of, must retain the above
*   copyright notice, this list of conditions and the following disclaimer.
*
*   Public listing of the source codes on the internet is not permitted.
*
*   Redistribution of the source codes or of the flanagan.jar file is not permitted.
*
*   Redistribution in binary form of all or parts of these classes is not permitted. 
*
*   Permission to use, copy and modify this software and its documentation for
*   NON-COMMERCIAL purposes is granted, without fee, provided that an acknowledgement
*   to the author, Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga, appears in all
*   copies and associated documentation or publications.
*
*   Dr Michael Thomas Flanagan makes no representations about the suitability
*   or fitness of the software for any or for a particular purpose.
*   Michael Thomas Flanagan shall not be liable for any damages suffered
*   as a result of using, modifying or distributing this software or its derivatives.
*
***************************************************************************************/

import flanagan.math.Fmath;
import flanagan.io.Db;
import flanagan.io.FileChooser;
import flanagan.io.FileOutput;
import flanagan.analysis.Regression;
import flanagan.analysis.RegressionFunction;
import flanagan.analysis.ProbabilityPlot;
import flanagan.analysis.Stat;
import flanagan.plot.PlotGraph;
import flanagan.interpolation.CubicSpline;

import javax.swing.JOptionPane;
import java.util.*;
import java.text.*;

public class GaussianFit{

    private int numberOfLines = 0;                  // number of lines in the in the data file
    private String title = null;                    // data title
    private String inputFileName = null;            // input file name
    private String outputFileName = null;           // output file name
    private int outputFileOption = 0;               // .txt or .xls file

    private int numberOfStudents = 0;               // number of students registered for the examination
    private int numberPresent = 0;                  // number of students present at the examination
    private int numberAbsent = 0;                   // number of students absent from the examination

    private double[] examMarks = null;              // examination marks used
    private double[] examMarksEntered = null;       // entered examination marks
    private double[] marks = null;                  // the centre of the marks for each histogram bin
    private double[] frequency = null;              // the number of marks in each histogram bin
    private int nBins = 0;                          // number of bins in the data histogram
    private double peakValue = -1;                  // frequency value for peak bin
    private double peakPosition = -1;               // position of peak bin
    private double binWidth = 0;                    // histogram bin width
    private int nBinPoints = 0;                     // number of histogram coordinates
    private double[] binXpoints = null;             // histogram x-coordinates
    private double[] binYpoints = null;             // histogram y-coordinates
    private boolean checkLimits = true;             // = false if no check on negative or >100 values required
    private ArrayList<Object> abs = new ArrayList<Object>();    // list of indices of and input word for missing marks

    private int scalingOption = 0;                  // = 0  no rescaling
                                                    // = 1  scaling factor
                                                    // = 2  new mean and standard deviation
    private double scalingFactor = 1.0;             // Scaling factor

    private double meanCalc = 0.0;                  // calculated mean of the marks
    private double standardDeviationCalc = 0.0;     // calculated standard deviation of the marks
    private double momentSkewness = 0.0;            // calculated moment skewness;
    private double medianSkewness = 0.0;            // calculated median skewness;
    private double quartileSkewness = 0.0;          // calculated quartile skewness;
    private double excessKurtosis = 0.0;            // calculated excess kurtosis;

    private double meanScaled = 0.0;                // scaled mean of the marks
    private double standardDeviationScaled = 0.0;   // scaled standard deviation of the marks
    private double momentSkewnessScaled= 0.0;       // scaled moment skewness;
    private double medianSkewnessScaled = 0.0;      // scaled median skewness;
    private double quartileSkewnessScaled = 0.0;    // scaled quartile skewness;
    private double excessKurtosisScaled = 0.0;      // scaled excess kurtosis;
    private boolean scaledCheck = false;            // = true if scaling performed

    private double meanUsed = 0.0;                  // used mean of the marks
    private double standardDeviationUsed = 0.0;     // used standard deviation of the marks

    private ProbabilityPlot probPlot = null;        // instance of probabilityPlot
    private double meanProbPlot = 0.0;              // best estimate mean from a probability plot
    private double standardDeviationProbPlot = 0.0; // best estimate standard deviation from a probability plot

    private Regression reg = null;                  // instance of Regression
    private double meanFit = 0.0;                   // best estimate mean (mu) from non-linear regression
    private double standardDeviationFit = 0.0;      // best estimate standard deviation (sigma) from non-linear regression
    private double scaleConstantFit = 0.0;          // best estimate scale constant (Ao) from non-linear regression
    private double[] calculatedFrequency = null;    // frequency values calculated for best estimates
    private double[] residuals = null;              // residuals

    private int trunc = 2;                          // precision (number of decimal places) in the output files
    private int field = 14;                         // output file field width


    // Read in and store the marks
    private void enterMarks(){

        // Choose data file
        FileChooser fin = new FileChooser();
        this.inputFileName = fin.selectFile();
        this.numberOfLines = fin.numberOfLines();
        ArrayList<Double> marksAL = new ArrayList<Double>();
        double holdD = 0.0;
        String holdS = null;

        if(this.numberOfLines==2){
            // individual marks on the same line in the data file
            this.title = fin.readLine();
            String line = fin.readLine();
            int lineLength = line.length();
            int ii = 0;
            int iStart = -1;
            int iLast = -1;
            this.numberOfStudents = 0;
            boolean test = true;
            if(line.charAt(ii)==' ' || line.charAt(ii)==',' || line.charAt(ii)==';'  || line.charAt(ii)==':' || line.charAt(ii)=='\t' || ii==lineLength-1){
                iStart = 0;
                iLast = 0;
            }
            ii++;

            while(test){
                if(line.charAt(ii)==' ' || line.charAt(ii)==',' || line.charAt(ii)==';'  || line.charAt(ii)==':' || line.charAt(ii)=='\t' || ii==lineLength-1){
                    iStart = ii;
                }
                if((iStart-iLast)>1){
                    if(ii==lineLength-1){
                        holdS = line.substring(iLast+1);
                    }
                    else{
                        holdS = line.substring(iLast+1,iStart);
                    }
                    try{
                        holdD = Double.parseDouble(holdS.trim());
                        marksAL.add(new Double(holdD));
                        this.numberPresent++;
                        this.numberOfStudents++;
                    }
                    catch(NumberFormatException e){
                        abs.add(new Integer(this.numberOfStudents));
                        abs.add(holdS);
                        this.numberAbsent++;
                        this.numberOfStudents++;

                    }
                    iLast = iStart;
                }
                ii++;
                if(ii>=lineLength)test=false;
            }
        }
        else{
            // individual marks on separate lines in the data file
            this.title = fin.readLine();
            this.numberOfStudents = numberOfLines - 1;
            for(int i=0; i<numberOfStudents; i++){
                holdS = fin.readLine();
                try{
                    holdD = Double.parseDouble(holdS.trim());
                    marksAL.add(new Double(holdD));
                    this.numberPresent++;
                }
                catch(NumberFormatException e){
                    this.numberAbsent++;
                    abs.add(new Integer(i));
                    abs.add(holdS);
                }
            }
        }

        this.examMarks = new double[numberPresent];
        this.examMarksEntered = new double[numberPresent];
        for(int i=0; i<numberPresent; i++){
            this.examMarks[i] = (marksAL.get(i)).doubleValue();
            this.examMarksEntered[i] = this.examMarks[i];
        }

        // Check for negative values and values greater than 100
        int flag = this.checkLimits(0);
    }

    // Check for negative values and values greater than 100
    private int checkLimits(int flag0){
        int flag1 = 0;
        int flag2 = 0;
        int flag3 = 0;
        for(int i=0; i<this.numberPresent; i++){
            if(this.examMarks[i]<0.0)flag1++;
            if(this.examMarks[i]>100.0)flag2++;
        }
        if(flag1>0)flag3 = 1;
        if(flag2>0)flag3 = 2;
        if(flag1>0 && flag2>0)flag3 = 3;
        String question = null;
        if(flag3>0 && this.checkLimits){
            if(flag0==0){
                switch(flag3){
                    case 1: question = "You have entered data in which there are negative values\n\n";
                            break;
                    case 2: question = "You have entered data in which there are values greater than 100.0\n\n";
                            break;
                    case 3: question = "You have entered data in which there are both negative values\n";
                            question += "and values greater than 100.0\n\n";
                            break;
                }
            }
            else{
                switch(flag3){
                    case 1: question = "You have introduced negative values on rescaling\n\n";
                            break;
                    case 2: question = "You have introduced values greater than 100.0 on rescaling\n\n";
                            break;
                    case 3: question = "You have introduced both negative values\n";
                            question += "and values greater than 100.0 on rescaling\n\n";
                            break;
                }
            }
            question += "Do you wish to set all negative values to zero\n";
            question += "and all values greater than 100.0 to 100.0\n\n";

            boolean answer = Db.yesNo(question);

            if(answer){
                for(int i=0; i<this.numberPresent; i++){
                    if(this.examMarks[i]<0.0)this.examMarks[i] = 0.0;
                    if(this.examMarks[i]>100.0)this.examMarks[i] = 100.0;
                }
            }
            else{
                this.checkLimits = false;
            }
        }
        return flag3;
    }


    //  Calculate statistics
    private void statistics(int flag){
        Stat st = new Stat(this.examMarks);

        if(flag==1){
            // unscaled data
            this.meanCalc = st.mean();
            this.meanUsed = this.meanCalc;
            this.standardDeviationCalc = st.standardDeviation();
            this.standardDeviationUsed = this.standardDeviationCalc;
            this.momentSkewness = st.momentSkewness();
            this.medianSkewness = st.medianSkewness();
            this.quartileSkewness = st.quartileSkewness();
            this.excessKurtosis = st.excessKurtosis();
        }
        else{
            // scaled data
            this.meanScaled = st.mean();
            this.meanUsed = this.meanScaled;
            this.standardDeviationScaled = st.standardDeviation();
            this.standardDeviationUsed = this.standardDeviationScaled;
            this.momentSkewnessScaled = st.momentSkewness();
            this.medianSkewnessScaled = st.medianSkewness();
            this.quartileSkewnessScaled = st.quartileSkewness();
            this.excessKurtosisScaled = st.excessKurtosis();
        }
    }

    // Offer scaling option
    private void scale(){

        String question = "The mean of the marks is " + Fmath.truncate(this.meanCalc, this.trunc) + "\n";
        question += "The standard deviation of the marks is " + Fmath.truncate(this.standardDeviationCalc, trunc) + "\n\n";
        question += "Do you want to rescale the examination marks?\n\n";
        boolean answer = Db.noYes(question);
        if(answer){

            String headerComment = "You may rescale by";
            String[] comments1 = {"a multiplicative scaling factor\n", "an additive or subtractive scaling factor\n", "entering a new mean and standard deviation\n"};
            String[] boxTitles1 = {"multiplicative factor", "additive or subtractive factor", "new mean and sd"};
            int defaultBox1 = 1;
            this.scalingOption =  Db.optionBox(headerComment, comments1, boxTitles1, defaultBox1);
            String message = null;
            switch(this.scalingOption){
                case 1: // Multiplicative scaling factor
                        message = "Enter the multiplicative scaling factor";
                        this.scalingFactor = Db.readDouble(message, 1.0);
                        for(int i=0; i<this.numberPresent; i++)this.examMarks[i] *= this.scalingFactor;
                        break;
                case 2: // Additive scaling factor
                        message = "Enter the additive or subtractive scaling factor\n";
                        message += "Enter a positive value for addition\n";
                        message += "Enter a negative value for subtraction\n";
                        this.scalingFactor = Db.readDouble(message);
                        for(int i=0; i<this.numberPresent; i++)this.examMarks[i] += this.scalingFactor;
                        break;
                case 3: // New mean and standard deviation
                        message = "The mean is " + Fmath.truncate(this.meanCalc, this.trunc) + "\n\n";
                        message += "Enter the new mean\n";
                        this.meanScaled = Db.readDouble(message, Fmath.truncate(this.meanCalc, this.trunc));

                        message = "The standard deviation is " + Fmath.truncate(this.standardDeviationCalc, this.trunc) + "\n\n";
                        message += "Enter the new standard deviation\n";
                        this.standardDeviationScaled = Db.readDouble(message, Fmath.truncate(this.standardDeviationCalc, this.trunc));

                        Stat sts = new Stat(this.examMarks);
                        this.examMarks = sts.scale(this.meanScaled, this.standardDeviationScaled);
            }

            // Check for introduction of negative values and values greater than 100
            int flag = this.checkLimits(1);

            this.scaledCheck = true;
            this.statistics(2);
        }
    }

    // Probability plot
    private void probabilityPlot(){

        // Create an instance of ProbabilityPlot
        this.probPlot = new ProbabilityPlot(this.examMarks);

        // Perform Gaussian probability plot fitting
        this.probPlot.gaussianProbabilityPlot();
        this.meanProbPlot = this.probPlot.gaussianMu();
        this.standardDeviationProbPlot = this.probPlot.gaussianSigma();
    }


    // Distribute data into histogram bins
    private void histogram(){
        double min = Fmath.minimum(this.examMarks);
        double max = Fmath.maximum(this.examMarks);
        double range = max - min;
        this.binWidth = Math.rint((2.0*this.standardDeviationUsed)/Math.sqrt(this.numberPresent));
        if(this.binWidth<2.0)this.binWidth = 2.0;

        // Decide on the histogram bin number and bin width
        String message = "The data has been arranged as an histogram\n";
        message += " needed for the data fitting procedure\n";
        message += "The suggested histogram bin width is " + this.binWidth + " marks\n\n";
        message += "Enter the histogram bin width";
        this.binWidth = Db.readDouble(message, this.binWidth);
        this.nBins = (int)Math.round(range/this.binWidth);
        this.binWidth = range/this.nBins;

        // Calculate bin heights
        double[] binStart = new double[this.nBins];
        double[] binEnd = new double[this.nBins];
        this.marks = new double[this.nBins];
        this.frequency = new double[this.nBins];
        binStart[0] = min;
        binEnd[0] = min + this.binWidth;
        this.marks[0] = (binEnd[0] + binStart[0])/2.0;
        for(int i=1; i<this.nBins; i++){
            binStart[i] = binEnd[i-1];
            binEnd[i] = binStart[i] + this.binWidth;
            this.marks[i] = (binEnd[i] + binStart[i])/2.0;
        }
        binStart[0] *= 0.8;
        binEnd[this.nBins-1] *= 1.2;
        int ii = 0;
        for(int i=0; i<this.nBins; i++){
            this.frequency[i] = 0;
            for(int j=0; j<this.numberPresent; j++){
                if(this.examMarks[j]>=binStart[i] && this.examMarks[j]<binEnd[i]){
                    frequency[i] += 1.0;
                    ii++;
                }
            }
        }
        if(ii!=this.numberPresent)System.out.println("The number of points in the histogram, " + ii + ", does not equal the number of examination marks, " + this.numberPresent);
        binStart[0] /= 0.8;
        binEnd[this.nBins-1] /= 1.2;

        // Histogram points
        this.nBinPoints = 3*this.nBins + 1;
        this.binXpoints = new double[this.nBinPoints];
        this.binYpoints = new double[this.nBinPoints];
        for(int i=0; i<this.nBins; i++){
            this.binXpoints[3*i] = binStart[i];
            this.binYpoints[3*i] = 0.0;
            this.binXpoints[3*i+1] = binStart[i];
            this.binYpoints[3*i+1] = this.frequency[i];
            this.binXpoints[3*i+2] = binEnd[i];
            this.binYpoints[3*i+2] = this.frequency[i];
        }
        this.binXpoints[this.nBinPoints-1] = binEnd[this.nBins-1];
        this.binYpoints[this.nBinPoints-1] = 0.0;;

        // Peak bin
        this.peakValue = Fmath.maximum(this.frequency);
        for(int i=0; i<this.nBins; i++){
            if(this.frequency[i]==this.peakValue){
                this.peakPosition = this.marks[i];
                break;
            }
        }
    }

    // Perform regression
    private void performRegression(){

        // Create an instance of Regression
        this.reg = new Regression(this.marks, this.frequency);

        // Create an instance of GaussFunction
        GaussFunction gf = new GaussFunction();

        // Initial estimates
        double[] start = new double[3];
        double[] step = new double[3];
        start[0] = this.peakPosition;
        if(this.peakPosition==0.0){
            step[0] = this.standardDeviationUsed/20.0;
        }
        else{
            step[0] = this.peakPosition/10.0;
        }
        start[1] = this.standardDeviationUsed;
        step[1] = this.standardDeviationUsed/10.0;
        start[2] = this.peakValue*this.standardDeviationUsed*Math.sqrt(2.0*Math.PI);
        step[2] = start[2]/10.0;

        this.reg.simplex(gf, start, step);

        double[] bestEstimates = this.reg.getBestEstimates();
        this.meanFit = bestEstimates[0];
        this.standardDeviationFit = bestEstimates[1];
        this.scaleConstantFit = bestEstimates[2];

        this.calculatedFrequency = this.reg.getYcalc();
        this.residuals = this.reg.getResiduals();

        // Plot regression fit
        double[][] data = PlotGraph.data(3, 200);
        CubicSpline cs = new CubicSpline(this.marks, this.calculatedFrequency);
        double[] xInterp = new double[200];
        double[] yInterp = new double[200];
        double inc = (this.marks[this.nBins-1] - this.marks[0])/199;
        xInterp[0] = this.marks[0];
        for(int i=1; i<199; i++)xInterp[i] = xInterp[i-1] + inc;
        xInterp[199] = this.marks[this.nBins-1];
        for(int i=0; i<200; i++)yInterp[i] = cs.interpolate(xInterp[i]);
        for(int i=0; i<this.nBins; i++){
            data[0][i] = this.marks[i];
            data[1][i] = this.frequency[i];
        }
        for(int i=0; i<200; i++){
            data[2][i] = xInterp[i];
            data[3][i] = yInterp[i];
        }
        for(int i=0; i<this.nBinPoints; i++){
            data[4][i] = this.binXpoints[i];
            data[5][i] = this.binYpoints[i];
        }

        PlotGraph pg = new PlotGraph(data);
        int[] lopt = {0,3,3};
        pg.setLine(lopt);
        int[] popt = {4,0,0};
        pg.setPoint(popt);
        pg.setGraphTitle("Program: GaussianFit.  Data File: " + this.inputFileName);
        pg.setGraphTitle2("Data title: " + this.title + ":  mu = " + Fmath.truncate(this.meanFit, this.trunc) + "    sigma = " + Fmath.truncate(this.standardDeviationFit, this.trunc));
        pg.setXaxisLegend("Marks");
        pg.setYaxisLegend("Frequency");
        pg.plot();
    }

    // Output methods
    // Select output file name and type
    private void output(){

        // Select type of output file
        String headerComment = "Which Analysis Output File type do you require?";
        String[] comments2 = {"Output as a text file (.txt)\n", "Output as an Excel readable file (.xls)                        .\n"};
        String[] boxTitles2 = {"text File (.txt)", "Excel File (.xls)"};
        int defaultBox = 1;
        this.outputFileOption =  Db.optionBox(headerComment, comments2, boxTitles2, defaultBox);
        String extn = ".txt";
        if(this.outputFileOption==2)extn = ".xls";


        // Create output file name
        this.outputFileName = null;
        String outputFileNameD = null;
        int pos = inputFileName.lastIndexOf('.');
        if(pos==-1){
            outputFileNameD = inputFileName+"Analysis"+extn;
        }
        else{
            outputFileNameD = inputFileName.substring(0,pos)+ "Analysis" + extn;
        }
        boolean checkExt = false;
        this.outputFileName = Db.readLine("Program GaussianFit:\n\nEnter the Analysis Output File name\n", outputFileNameD);
        if(!this.outputFileName.equals(outputFileNameD)){
            String outputFileNameHold = this.outputFileName;
            pos = this.outputFileName.lastIndexOf('.');
            if(pos==-1){
                    this.outputFileName += extn;
                    checkExt = true;
            }
            else{
                if(this.outputFileOption==1){
                    if(!(this.outputFileName.substring(pos)).equals(".txt")){
                        this.outputFileName = this.outputFileName.substring(0,pos) + extn;
                        checkExt = true;
                    }
                }
                else{
                    if(!(this.outputFileName.substring(pos)).equals(".xls")){
                        this.outputFileName = this.outputFileName.substring(0,pos) + extn;
                        checkExt = true;
                    }
                }
            }
            if(checkExt){
                String message = null;
                if(this.outputFileOption==1){
                    message = "You chose a text file (.txt) as the output file type\n";
                }
                else{
                    message = "You chose an Excel readable file (.xls) as the output file type\n";
                }
                message += "consequently your file name, '" + outputFileNameHold + "', has been\n";
                message += "changed to '" + this.outputFileName + "'\n";
                JOptionPane.showMessageDialog(null, message, "Program GaussianFit: Output File", JOptionPane.WARNING_MESSAGE);
            }
        }

        // Output the results
        this.outputText();
    }

    // Output analysis as a .txt or .xls file
    private void outputText(){

        // create an instance of FileOutput
        FileOutput fout = new FileOutput(this.outputFileName);

        // output title
        fout.println("FITTING EXAMINATION MARKS TO A GAUSSIAN DISTRIBUTION");
        fout.println("Program: GaussianFit");
        fout.println("Data title: " + this.title);
        fout.println("Data read from input file: " + this.inputFileName);

        Date d = new Date();
        String day = DateFormat.getDateInstance().format(d);
        String tim = DateFormat.getTimeInstance().format(d);
        fout.println("Program executed at " + tim + " on " + day);
        fout.println();
        fout.println("Number of students:            " + this.numberOfStudents);
        fout.println("Number of students present:    " + this.numberPresent);
        fout.println("Number of students absent:     " + this.numberAbsent);
        fout.println();

        // Output statistics
        if(this.scaledCheck){
            fout.printtab("Statistic", 30);
            fout.printtab("Entered", this.field);
            fout.printtab("Scaled", this.field);
            fout.printtab("Probability", this.field);
            fout.println("Non-linear");
            fout.printtab("  ", 30);
            fout.printtab("marks", this.field);
            fout.printtab("marks", this.field);
            fout.printtab("plot", this.field);
            fout.println("regression");

            fout.printtab("mean / mu", 30);
            fout.printtab(Fmath.truncate(this.meanCalc, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.meanScaled, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.meanProbPlot, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.meanFit, this.trunc), this.field);
            fout.println();
            fout.printtab("standard deviation / sigma", 30);
            fout.printtab(Fmath.truncate(this.standardDeviationCalc, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.standardDeviationScaled, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.standardDeviationProbPlot, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.standardDeviationFit, this.trunc), this.field);
            fout.println();
            fout.printtab("moment skewness", 30);
            fout.printtab(Fmath.truncate(this.momentSkewness, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.momentSkewnessScaled, this.trunc), this.field);
            fout.println();
            fout.printtab("median skewness", 30);
            fout.printtab(Fmath.truncate(this.medianSkewness, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.medianSkewnessScaled, this.trunc), this.field);
            fout.println();
            fout.printtab("quartile skewness", 30);
            fout.printtab(Fmath.truncate(this.quartileSkewness, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.quartileSkewnessScaled, this.trunc), this.field);
            fout.println();
            fout.printtab("excess kurtosis", 30);
            fout.printtab(Fmath.truncate(this.excessKurtosis, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.excessKurtosisScaled, this.trunc), this.field);
            fout.println();
        }
        else{
            fout.printtab("Statistic", 30);
            fout.printtab("Entered", this.field);
            fout.printtab("Probability", this.field);
            fout.println("Non-linear");
            fout.printtab("  ", 30);
            fout.printtab("marks", this.field);
            fout.printtab("plot", this.field);
            fout.println("regression");

            fout.printtab("mean / mu", 30);
            fout.printtab(Fmath.truncate(this.meanCalc, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.meanProbPlot, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.meanFit, this.trunc), this.field);
            fout.println();
            fout.printtab("standard deviation / sigma", 30);
            fout.printtab(Fmath.truncate(this.standardDeviationCalc, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.standardDeviationProbPlot, this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.standardDeviationFit, this.trunc), this.field);
            fout.println();
            fout.printtab("moment skewness", 30);
            fout.printtab(Fmath.truncate(this.momentSkewness, this.trunc), this.field);
            fout.println();
            fout.printtab("median skewness", 30);
            fout.printtab(Fmath.truncate(this.medianSkewness, this.trunc), this.field);
            fout.println();
            fout.printtab("quartile skewness", 30);
            fout.printtab(Fmath.truncate(this.quartileSkewness, this.trunc), this.field);
            fout.println();
            fout.printtab("excess kurtosis", 30);
            fout.printtab(Fmath.truncate(this.excessKurtosis, this.trunc), this.field);
            fout.println();
        }

        fout.println();
        fout.println();


        // Probability plot analysis
        fout.println("PROBABILITY PLOT ANALYSIS");
        fout.println();
        double rr = this.probPlot.gaussianCorrelationCoefficient();
        fout.println("Correlation coefficient: R = " + Fmath.truncate(rr, this.trunc) + "     R squared = " + Fmath.truncate(rr*rr, this.trunc));
        fout.println("Sum of squares of the residuals: " + Fmath.truncate(this.probPlot.gaussianSumOfSquares(), this.trunc));
        fout.println();

        fout.printtab(" ", this.field);
        fout.printtab("Value", this.field);
        fout.printtab("Error", this.field);
        fout.println("Coefficient of");
        fout.printtab(" ", this.field);
        fout.printtab(" ", this.field);
        fout.printtab(" ", this.field);
        fout.println("variation (%)");

        fout.printtab("Gradient", this.field);
        double grad = this.probPlot.gaussianGradient();
        double gradError = this.probPlot.gaussianGradientError();
        fout.printtab(Fmath.truncate(grad, this.trunc), this.field);
        fout.printtab(Fmath.truncate(gradError, this.trunc),this.field);
        fout.println(Fmath.truncate(Math.abs(gradError*100.0/grad), this.trunc));

        double intr = this.probPlot.gaussianIntercept();
        double intrError = this.probPlot.gaussianInterceptError();
        fout.printtab("Intercept", this.field);
        fout.printtab(Fmath.truncate(intr, this.trunc), this.field);
        fout.printtab(Fmath.truncate(intrError, this.trunc), this.field);
        fout.println(Fmath.truncate(Math.abs(intrError*100/intr), this.trunc));
        fout.println();

        fout.printtab(" ", this.field);
        fout.printtab("Best", this.field);
        fout.printtab("Error", this.field);
        fout.printtab("Coefficient of", this.field);
        fout.printtab("t-value  ", this.field);
        fout.println("p-value");

        fout.printtab(" ", this.field);
        fout.printtab("Estimate", this.field);
        fout.printtab("        ", this.field);
        fout.printtab("variation (%)", this.field);
        fout.printtab("t ", this.field);
        fout.println("P > |t|");

        double muError = this.probPlot.gaussianMuError();
        fout.printtab("mu", this.field);
        fout.printtab(Fmath.truncate(this.meanProbPlot, this.trunc), this.field);
        fout.printtab(Fmath.truncate(muError, this.trunc), this.field);
        fout.printtab(Fmath.truncate(Math.abs(100.0*muError/this.meanProbPlot), this.trunc), this.field);
        double tVal = this.meanProbPlot/muError;
        fout.printtab(Fmath.truncate(tVal, this.trunc), this.field);
        double pVal =  1.0 - Stat.studentTcdf(-tVal, tVal, this.nBins-2);
        fout.println(Fmath.truncate(pVal, this.trunc));

        double sigmaError = this.probPlot.gaussianSigmaError();
        fout.printtab("sigma", this.field);
        fout.printtab(Fmath.truncate(this.standardDeviationProbPlot, this.trunc), this.field);
        fout.printtab(Fmath.truncate(sigmaError, this.trunc), this.field);
        fout.printtab(Fmath.truncate(Math.abs(100.0*sigmaError/this.standardDeviationProbPlot), this.trunc), this.field);
        tVal = this.standardDeviationProbPlot/sigmaError;
        fout.printtab(Fmath.truncate(tVal, this.trunc), this.field);
        pVal =  1.0 - Stat.studentTcdf(-tVal, tVal, this.nBins-2);
        fout.println(Fmath.truncate(pVal, this.trunc));
        fout.println();

        fout.printtab("Ordered data", this.field);
        fout.printtab("Gaussian order", this.field);
        fout.printtab("Residuals", this.field);
        fout.printtab("Ordered data", this.field);
        fout.printtab("Gaussian order", this.field);
        fout.printtab("Residuals", this.field);
        fout.printtab("Ordered data", this.field);
        fout.printtab("Gaussian order", this.field);
        fout.println("Residuals");

        fout.printtab("values", this.field);
        fout.printtab("statistic", this.field);
        fout.printtab("         ", this.field);
        fout.printtab("values", this.field);
        fout.printtab("statistic", this.field);
        fout.printtab("         ", this.field);
        fout.printtab("values", this.field);
        fout.printtab("statistic", this.field);
        fout.println();

        fout.printtab("      ", this.field);
        fout.printtab("medians", this.field);
        fout.printtab("         ", this.field);
        fout.printtab("      ", this.field);
        fout.printtab("medians", this.field);
        fout.printtab("         ", this.field);
        fout.printtab("      ", this.field);
        fout.printtab("medians", this.field);
        fout.println();

        double[] odv = this.probPlot.gaussianOrderStatisticMedians();
        double[] oda = this.probPlot.orderedData();
        int ii = 0;
        for(int i=0; i<this.numberPresent; i++){
            if(ii<2){
                fout.printtab(Fmath.truncate(oda[i], this.trunc), this.field);
                fout.printtab(Fmath.truncate(odv[i], this.trunc), this.field);
                fout.printtab(Fmath.truncate(oda[i] - odv[i], this.trunc), this.field);
            }
            else{
                fout.printtab(Fmath.truncate(oda[i], this.trunc), this.field);
                fout.printtab(Fmath.truncate(odv[i], this.trunc), this.field);
                fout.println(Fmath.truncate(oda[i] - odv[i], this.trunc));
            }
            ii++;
            if(ii==3)ii=0;
        }
        fout.println();
        fout.println();
        fout.println();



        // Non-linear regression analysis
        fout.println("NON-LINEAR REGRESSION ANALYSIS");
        fout.println();
        double lcyy = this.reg.getYYcorrCoeff();
        fout.println("Linear correlation coefficient: experimental y data versus calculated y data");
        fout.println("R = " + Fmath.truncate(lcyy, this.trunc) + "     R squared = " + Fmath.truncate(lcyy*lcyy, this.trunc));

        double cod = this.reg.getCoefficientOfDetermination();
        fout.println("Coefficient of determination:");
        fout.println("R = " + Fmath.truncate(Math.sqrt(cod), this.trunc) + "     R squared = " + Fmath.truncate(cod, this.trunc));

        double acod = this.reg.getAdjustedCoefficientOfDetermination();
        fout.println("Adjusted coefficient of determination:");
        fout.println("R = " + Fmath.truncate(Math.sqrt(acod), this.trunc) + "     R squared = " + Fmath.truncate(acod, this.trunc));

        double fcod = this.reg.getCoeffDeterminationFratio();
        fout.println("Coefficient of determination F-ratio:");
        fout.println("F = " + Fmath.truncate(fcod, this.trunc));

        double pfcod = this.reg.getCoeffDeterminationFratioProb();
        fout.println("Coefficient of determination F-ratio probability:");
        fout.println("P = " + Fmath.truncate(pfcod, this.trunc));

        double ss = this.reg.getSumOfSquares();
        fout.println("Sum of squares of residuals: "+ Fmath.truncate(ss, this.trunc));

        double dof = this.reg.getDegFree();
        fout.println("Degrees of freedom:          "+ Fmath.truncate(dof, this.trunc));
        fout.println();

        double[] errors = this.reg.getBestEstimatesErrors();
        double[] tValF = this.reg.getTvalues();
        double[] pValF = this.reg.getPvalues();
        fout.printtab(" ", this.field);
        fout.printtab("Best", this.field);
        fout.printtab("Error", this.field);
        fout.printtab("Coefficient of", this.field);
        fout.printtab("t-value  ", this.field);
        fout.println("p-value");

        fout.printtab(" ", this.field);
        fout.printtab("Estimate", this.field);
        fout.printtab("        ", this.field);
        fout.printtab("variation (%)", this.field);
        fout.printtab("t ", this.field);
        fout.println("P > |t|");

        fout.printtab("mu", this.field);
        fout.printtab(Fmath.truncate(this.meanFit, this.trunc), this.field);
        fout.printtab(Fmath.truncate(errors[0], this.trunc), this.field);
        fout.printtab(Fmath.truncate(Math.abs(100.0*errors[0]/this.meanFit), this.trunc), this.field);
        fout.printtab(Fmath.truncate(tValF[0], this.trunc), this.field);
        fout.println(Fmath.truncate(pValF[0], this.trunc));

        fout.printtab("sigma", this.field);
        fout.printtab(Fmath.truncate(this.standardDeviationFit, this.trunc), this.field);
        fout.printtab(Fmath.truncate(errors[1], this.trunc), this.field);
        fout.printtab(Fmath.truncate(Math.abs(100.0*errors[1]/this.standardDeviationFit), this.trunc), this.field);
        fout.printtab(Fmath.truncate(tValF[1], this.trunc), this.field);
        fout.println(Fmath.truncate(pValF[1], this.trunc));

        fout.printtab("Ao", this.field);
        fout.printtab(Fmath.truncate(this.scaleConstantFit, this.trunc), this.field);
        fout.printtab(Fmath.truncate(errors[2], this.trunc), this.field);
        fout.printtab(Fmath.truncate(Math.abs(100.0*errors[2]/this.scaleConstantFit), this.trunc), this.field);
        fout.printtab(Fmath.truncate(tValF[2], this.trunc), this.field);
        fout.println(Fmath.truncate(pValF[2], this.trunc));
        fout.println();

        fout.println("Binned marks");
        fout.println("Bin width: " + this.binWidth);
        fout.printtab("Bin", this.field);
        fout.printtab("Experimental", this.field);
        fout.printtab("Calculated", this.field);
        fout.println("Residuals");
        fout.printtab("centre", this.field);
        fout.printtab("frequency", this.field);
        fout.println("frequency");

        for(int i=0; i<this.nBins; i++){
            fout.printtab(Fmath.truncate(this.marks[i], this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.frequency[i], this.trunc), this.field);
            fout.printtab(Fmath.truncate(this.calculatedFrequency[i], this.trunc), this.field);
            fout.println(Fmath.truncate(this.residuals[i], this.trunc));
        }
        fout.println();

        fout.println();
        fout.println();


        // Output data used
        fout.println("DATA");
        if(!this.scaledCheck){
            this.field = 9;
            fout.println("Examination marks used");
            ii = 0;
            for(int i=0; i<this.numberPresent; i++){
                if(ii<8){
                    fout.printtab(Fmath.truncate(this.examMarks[i], this.trunc), this.field);
                }
                else{
                    fout.println(Fmath.truncate(this.examMarks[i], this.trunc));
                }
                ii++;
                if(ii==9)ii = 0;
            }
            fout.println();
            fout.println();
        }
        else{
            fout.println("The data was scaled");
            switch(this.scalingOption){
                case 1: fout.println("Multiplicative scaling factor = " + this.scalingFactor);
                        break;
                case 2: fout.println("Additive scaling factor = " + this.scalingFactor);
                        break;
                case 3: fout.println("Scaled to a mean                " + Fmath.truncate(this.meanScaled, this.trunc));
                        fout.println("Scaled to a standard deviation  " + Fmath.truncate(this.standardDeviationScaled, this.trunc));
                        break;
            }
            fout.println();

            fout.printtab("Original", this.field);
            fout.println("Scaled");
            fout.printtab("marks", this.field);
            fout.println("marks");
            if(this.numberAbsent==0){
                for(int i=0; i<this.numberPresent; i++){
                    fout.printtab(Fmath.truncate(this.examMarksEntered[i], this.trunc), this.field);
                    fout.println(Fmath.truncate(this.examMarks[i], this.trunc));
                }
            }
            else{
                ii = -1;
                int counter = 0;
                boolean checkAbsSize = true;
                int nAbs = abs.size();
                int holdAI = ((Integer)abs.get(++ii)).intValue();
                String holdAS =(String)abs.get(++ii);
                for(int i=0; i<this.numberOfStudents; i++){
                    if(checkAbsSize && i==holdAI){
                            fout.printtab(holdAS,this.field);
                            fout.println(holdAS);
                            if(ii==nAbs-1){
                                checkAbsSize = false;
                            }
                            else{
                                holdAI = ((Integer)abs.get(++ii)).intValue();
                                holdAS =(String)abs.get(++ii);
                            }
                    }
                    else{
                        fout.printtab(Fmath.truncate(this.examMarksEntered[counter], this.trunc), this.field);
                        fout.println(Fmath.truncate(this.examMarks[counter], this.trunc));
                        counter++;
                    }
                }
            }
        }
        fout.close();
    }

    // Output analysis as an Excel readable file
    private void outputExcel(){
    }




    // Main method
    public static void main(String[] arg){

        // Introductory message
        String message = "This program fits exanination marks to a Gaussian distribution. It accepts the marks\n";
        message += "from a text file and requests information about the marks through a series of dialogue boxes.\n\n";
        message += "Please select the marks data file from the file select window and respond to each dialogue box\n";
        message += "that appears on closing this message (by clicking on the OK button)";
        JOptionPane.showMessageDialog(null, message, "Program GaussianFit: Introduction", JOptionPane.INFORMATION_MESSAGE);

        // Create instance of GaussianFit
        GaussianFit ftg = new GaussianFit();

        // Read in and store the marks
        ftg.enterMarks();

        // Calculate statistics
        ftg.statistics(1);

        // Offer a scaling option
        ftg.scale();

        // Probabilty plot
        ftg.probabilityPlot();

        // Distribute the data into histogram bins
        ftg.histogram();

        // Perform Regression
        ftg.performRegression();

        // Output the analysis
        ftg.output();

        // end program
        String question = "You may end the program now or later.\n\n";
        question += "If you click YES the program will be terminated and the graph windows will close.\n\n";
        question += "If you click NO the graph windows will remain open\n";
        question += "You must then terminate the program later by clicking on the close icon\n";
        question += "(white cross on red background in the top right hand corner) on any of the plots\n\n";

        boolean answer = Db.yesNo(question);

        if(answer)System.exit(0);

    }

}

// Class to evaluate the Gausian (normal) function y = (yscale/sd.sqrt(2.pi)).exp(-0.5[(x - xmean)/sd]^2).
class GaussFunction implements RegressionFunction{
    public double function(double[] p, double[] x){
        return (p[2]/(p[1]*Math.sqrt(2.0D*Math.PI)))*Math.exp(-0.5D*Fmath.square((x[0]-p[0])/p[1]));
    }
}
