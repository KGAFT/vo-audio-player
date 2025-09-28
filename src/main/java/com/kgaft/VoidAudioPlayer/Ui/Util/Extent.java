package com.kgaft.VoidAudioPlayer.Ui.Util;

public class Extent {
    public int width;
    public int height;

    public Extent(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Extent() {
    }

    @Override
    public int hashCode() {
        return width*2+height;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass().equals(this.getClass())){
            return ((Extent)obj).width == this.width && ((Extent)obj).height == this.height;
        }
        return false;
    }
}
