package com.hakaselabs.speech2emoji;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by wei on 16/11/15.
 */
public class Candidate implements Comparable<Candidate> {
    private Integer id;
    private double logProb;


    public Candidate(Integer id, double prob){
        this.id = id;
        this.logProb = Math.log(prob);
    }

    public Candidate(Integer id, float prob){
        this(id, (double) prob);
    }

    public int getId(){
        return this.id;
    }

    public double getProb(){
        return Math.exp(logProb);
    }

    public int compareTo(Candidate c2){
        double v = this.getProb() - c2.getProb();
        return v > 0 ? 1 : -1;
    }

    public String toString(){
        return this.id+":"+this.getProb();
    }

    public boolean equals(Object that) {
        if (this == that) return true;
        if(!(that instanceof Candidate) )
            return false;

        Candidate thatCandidate = (Candidate) that;
        return thatCandidate.getId() == this.getId();
    }

    public static void main(String[] args) {
        Random r = new Random();
        List<Candidate> cList = new ArrayList<Candidate>();
        for (int i = 0; i < 10; i++) {
            double d = r.nextDouble();
            Integer id = i;
            Candidate c = new Candidate(id, d);
            cList.add(c);
        }
        Collections.sort(cList);
        for(Candidate c : cList){
            System.out.println(c);
        }
        System.out.println();

    }
}
