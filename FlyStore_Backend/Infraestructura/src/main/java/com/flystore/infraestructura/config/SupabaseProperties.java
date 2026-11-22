package com.flystore.infraestructura.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración para Supabase Lee las propiedades del archivo
 * application.properties con prefijo "supabase"
 */
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    private String url;
    private String publishableKey;
    private String secretKey;
    private String storageBucket = "productos";
    /** Bucket privado de comprobantes de pago (CU-02). */
    private String comprobantesBucket = "comprobantes";

    public String getStorageBucket() { return storageBucket; }
    public void setStorageBucket(String storageBucket) { this.storageBucket = storageBucket; }

    public String getComprobantesBucket() { return comprobantesBucket; }
    public void setComprobantesBucket(String comprobantesBucket) { this.comprobantesBucket = comprobantesBucket; }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

}
