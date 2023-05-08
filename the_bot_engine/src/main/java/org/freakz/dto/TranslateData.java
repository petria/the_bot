package org.freakz.dto;

import java.io.Serializable;

/**
 * Created by Petri Airio on 12.11.2015.
 * -
 */
public class TranslateData implements Serializable {

    private String translation;
    private String context;

    public TranslateData() {
    }

    public TranslateData(String translation, String context) {
        this.translation = translation;
        this.context = context;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

}
