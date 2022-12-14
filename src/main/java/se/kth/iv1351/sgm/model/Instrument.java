package se.kth.iv1351.sgm.model;

public class Instrument implements InstrumentDTO{
    private int id;
    private int price;
    private String type;
    private String brand;
    private String quality;

    public Instrument(int id, int price, String type, String brand, String quality){
        this.id = id;
        this.price = price;
        this.type = type;
        this.brand = brand;
        this.quality = quality;
    }

    @Override
    public String toString(){
        return "id: " + getId() +
                ", price: " + getPrice() +
                //", type: " + getType() +
                ", brand: " + getBrand() +
                ", quality: " + getQuality();
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getPrice() {
        return this.price;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getBrand() {
        return this.brand;
    }

    @Override
    public String getQuality() {
        return this.quality;
    }
}
