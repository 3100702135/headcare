package com.example.administrator.headcare.bean;

public class Point {
    public int X;
    public int Y;

    public Point(int i, int y) {
       this.setX(i);
       this.setY(y);
    }

    public int getX() {
        return X;
    }

    public void setX(int x) {
        X = x;
    }

    public int getY() {
        return Y;
    }

    public void setY(int y) {
        Y = y;
    }
}
