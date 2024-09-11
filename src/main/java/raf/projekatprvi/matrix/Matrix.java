package main.java.raf.projekatprvi.matrix;

import java.io.File;
import java.math.BigInteger;

public class Matrix {
    private String name;
    private int rows;
    private int columns;
    private BigInteger[][] data;

    private File file;

    private String multMatrix;

    public Matrix() {
    }

    public Matrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.data = new BigInteger[rows][columns];
    }

    public Matrix(String name, int rows, int columns) {
        this.name = name;
        this.rows = rows;
        this.columns = columns;
        this.data = new BigInteger[rows][columns];
    }


    public void set(int row, int col, BigInteger value) {
        if (row < 0 || row > rows || col < 0 || col > columns) {
            throw new IndexOutOfBoundsException("Row or column index out of bounds");
        }
        data[row][col] = value;
    }

    public BigInteger getValueAt(int i, int j) {
        if (i < 0 || i >= rows || j < 0 || j >= columns) {
            throw new IllegalArgumentException("Pozicija (" + i + ", " + j + ") je van granica matrice.");
        }
        return data[i][j];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public BigInteger[][] getData() {
        return data;
    }

    public void setData(BigInteger[][] data) {
        this.data = data;
    }

    public String getInfo() {
        return " IME MATRICE: " + name + " REDOVI: " + rows  + " KOLONE: "+ columns + " FAJL: " + file;
    }


    public void fillWithZeros(int rows,int columns) {
        this.data = new BigInteger[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                data[i][j] = BigInteger.ZERO;
            }
        }
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getMultMatrix() {
        return multMatrix;
    }

    public void setMultMatrix(String multMatrix) {
        this.multMatrix = multMatrix;
    }
}
