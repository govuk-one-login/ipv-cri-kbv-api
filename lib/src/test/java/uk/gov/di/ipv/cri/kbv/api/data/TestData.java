package uk.gov.di.ipv.cri.kbv.api.data;

public final class TestData {
    public static final String APERSONIDENTITY =
            "{\n"
                    + "      \"dateOfBirth\": \"0200-01-10\",\n"
                    + "      \"surname\": \"JOE\",\n"
                    + "      \"firstName\": \"BLOGGS\",\n"
                    + "      \"addresses\": [\n"
                    + "        {\n"
                    + "          \"addressType\": \"CURRENT\",\n"
                    + "          \"houseNumber\": \"1\",\n"
                    + "          \"townCity\": \"EURASIA\",\n"
                    + "          \"street\": \"LONG BEFORE CLIMATE CHANGE\",\n"
                    + "          \"postcode\": \"THE CAVE\"\n"
                    + "        }\n"
                    + "      ]\n"
                    + "    }";
    ;
    public static final String GOODJWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2MyaWQuY29tIiwic3ViIjoiYWxpY2UiLCJjbGFpbSI6eyJ2Y19odHRwX2FwaSI6eyJkYXRlT2ZCaXJ0aCI6IjAyMDAtMDEtMTAiLCJzdXJuYW1lIjoiSk9FIiwiaXB2X3Nlc3Npb25faWQiOiJhZWE1NzczNS0xNjk4LTQzYmEtODBiZi1lYjJkYWVkMDk1M2YiLCJmaXJzdE5hbWUiOiJCTE9HR1MiLCJhZGRyZXNzZXMiOlt7ImFkZHJlc3NUeXBlIjoiQ1VSUkVOVCIsImhvdXNlTnVtYmVyIjoiMSIsInRvd25DaXR5IjoiRVVSQVNJQSIsInN0cmVldCI6IkxPTkcgQkVGT1JFIENMSU1BVEUgQ0hBTkdFIiwicG9zdGNvZGUiOiJUSEUgQ0FWRSJ9XX19LCJpYXQiOjE2NDMyMDg3NjN9.Af1wIpCzaLYhUpXHTlizKFeNBprN_4C_odAjXmEBoXA";
    public static final String BADJWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2MyaWQuY29tIiwic3ViIjoiYWxpY2UiLCJjbGFpbSI6eyJ2Y19odHRwX2FwaSI6eyJkYXRlT2ZCaXJ0aCI6IjAyMDAtMDEtMTAiLCJzdXJuYW1lIjoiSk9FIiwiaXB2X3Nlc3Npb25faWQiOiJhZWE1NzczNS0xNjk4LTQzYmEtODBiZi1lYjJkYWVkMDk1M2YiLCJmaXJzdE5hbWUxIjoiQkxPR0dTIiwiYWRkcmVzc2VzIjpbeyJhZGRyZXNzVHlwZSI6IkNVUlJFTlQiLCJob3VzZU51bWJlciI6IjEiLCJ0b3duQ2l0eSI6IkVVUkFTSUEiLCJzdHJlZXQiOiJMT05HIEJFRk9SRSBDTElNQVRFIENIQU5HRSIsInBvc3Rjb2RlIjoiVEhFIENBVkUifV19fSwiaWF0IjoxNjQzMjA4NzYzfQ.4bx-LehbpOq7y8cGNTTbPoZExNWFA0pjQyFPdXq9NxE";
}
