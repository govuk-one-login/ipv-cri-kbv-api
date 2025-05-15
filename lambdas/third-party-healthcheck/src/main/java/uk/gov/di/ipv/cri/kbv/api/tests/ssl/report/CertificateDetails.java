package uk.gov.di.ipv.cri.kbv.api.tests.ssl.report;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;

public class CertificateDetails {
    private final int certificateNumber;
    private final String subject;
    private final String issuer;
    private final String serialNumber;
    private final Date validFrom;
    private final Date validUntil;
    private final String signatureAlgorithm;
    private final String publicKeyAlgorithm;
    private final int keyLength;
    private final String version;
    private final String thumbprint; // SHA-256 fingerprint

    public CertificateDetails(X509Certificate cert, int number) {
        Objects.requireNonNull(cert, "Certificate must not be null");

        this.certificateNumber = number;
        this.subject = cert.getSubjectX500Principal().getName();
        this.issuer = cert.getIssuerX500Principal().getName();
        this.serialNumber = cert.getSerialNumber().toString(16).toUpperCase();
        this.validFrom = new Date(cert.getNotBefore().getTime());
        this.validUntil = new Date(cert.getNotAfter().getTime());
        this.signatureAlgorithm = cert.getSigAlgName();
        this.publicKeyAlgorithm = cert.getPublicKey().getAlgorithm();
        this.keyLength = extractKeyLength(cert);
        this.version = "V" + cert.getVersion();
        this.thumbprint = calculateThumbprint(cert);
    }

    public int getCertificateNumber() {
        return certificateNumber;
    }

    public String getSubject() {
        return subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public Date getValidFrom() {
        return new Date(validFrom.getTime());
    }

    public Date getValidUntil() {
        return new Date(validUntil.getTime());
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public String getPublicKeyAlgorithm() {
        return publicKeyAlgorithm;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public String getVersion() {
        return version;
    }

    public String getThumbprint() {
        return thumbprint;
    }

    private int extractKeyLength(X509Certificate cert) {
        try {
            String keyFormat = cert.getPublicKey().toString();
            if (keyFormat.contains("RSA")) {
                int bitsIndex = keyFormat.indexOf("bit");
                if (bitsIndex > 0) {
                    String bitString = keyFormat.substring(0, bitsIndex).trim();
                    return Integer.parseInt(bitString.substring(bitString.lastIndexOf(" ") + 1));
                }
            } else if (keyFormat.contains("EC")) {
                return -1;
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private String calculateThumbprint(X509Certificate cert) {
        try {
            byte[] encoded = cert.getEncoded();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(encoded);
            return bytesToHex(hash);
        } catch (Exception e) {
            return "Unable to calculate thumbprint: " + e.getMessage();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
