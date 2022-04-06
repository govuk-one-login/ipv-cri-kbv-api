package uk.gov.di.ipv.cri.kbv.api.library.data;

public final class TestData {

    public static final String GOODJWT =
            "eyJraWQiOiJpcHYtY29yZS1zdHViIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJ1cm46dXVpZDpiMzRjNjQwMi1kZmVjLTRjMjItYTkzOS1lZDJkZjBhM2MzZjUiLCJhdWQiOiJodHRwczpcL1wvZXhwZXJpYW4uY3JpLmFjY291bnQuZ292LnVrIiwibmJmIjoxNjQ5MTc0NTg3LCJzaGFyZWRfY2xhaW1zIjp7ImJpcnRoRGF0ZSI6W3sidmFsdWUiOiIxOTY0LTA5LTAzIn1dLCJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6IktFTk5FVEgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJERUNFUlFVRUlSQSJ9XX1dLCJhZGRyZXNzZXMiOlt7ImN1cnJlbnRBZGRyZXNzIjp0cnVlLCJ0b3duQ2l0eSI6IkJBVEgiLCJzdHJlZXQyIjoiSEFETEVZIFJPQUQiLCJzdHJlZXQxIjoiOCIsInBvc3RDb2RlIjoiQkEyIDVBQSJ9XSwiQGNvbnRleHQiOlsiaHR0cHM6XC9cL3d3dy53My5vcmdcLzIwMThcL2NyZWRlbnRpYWxzXC92MSIsImh0dHBzOlwvXC92b2NhYi5sb25kb24uY2xvdWRhcHBzLmRpZ2l0YWxcL2NvbnRleHRzXC9pZGVudGl0eS12MS5qc29ubGQiXX0sImlzcyI6Imh0dHBzOlwvXC9kZXYuY29yZS5pcHYuYWNjb3VudC5nb3YudWsiLCJleHAiOjE2NDkxNzgxODcsImlhdCI6MTY0OTE3NDU4N30.nLgsh1j_W-iU1qNM-slrwRbK3PfZ2EOnht8jOriTsmu8qdZju-JxAeM612EC-6eMvmqi92shBVUFjrJhC9wN7UVFYaRaFmyk-IZY5fcUEdOnebz9bj5b9O7S_hRERgA8SScy4yVuy5PHXvueQjq4p5Hx-wNz-JA5NcqV4bkCAN7FTExLRTLPWoRsUdKvrxLOrLFfBs7LSv03kTslZwV4_SSZrUnCUellSr_pqDaMIc9qp9TdS6IYOSL14tfoSt_4ccIAFR-wZ8nFdj9nNk_tmGENRs8wEkncaHMSut40tAeHwjN2cCJd2DwIp-Y4OyJbV413Y0e7sub6p1BrEh1qlN1zAncMmMXOu5ak3GGJbfEJTbJoI58O5Ep4wueFA_qDXAivCbH_-MLjkDGsDoIdAXJbbO-ZkmDM3Cs0Y6gRuPv6v3lkejW5Q6joJ0-QNLQj5dcK_vQOHt9FzPaLBaIr03KebCuSjL08bCOnl4G67icgxzxbI3ci_Y1KkR1ucRJYG0DFjCGGocKy7mSB2zwOpPeUq66SYlf__c5DXhTdzl5kh6Xa5PsESaX9lD33SQLYSS0mqBQ3RMPFEp2iMnJWjhc4gOZdyJDAcrIntxkn_DN8qVFphDAeYrv2IfOz4Mo7iEpJpRYKJscaNufZOARUylB-ih_-2DGpgkgBrQ0hxqI";
    public static final String BADJWT =
            "eyJraWQiOiJpcHYtY29yZS1zdHViIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJ1cm46dXVpZDpkZTc1ZjM5Ny0xYzdlLTQ0NDMtYWUzNi0xZGE3Njk3YzYzNzciLCJhdWQiOiJodHRwczpcL1wvZXhwZXJpYW4uY3JpLmFjY291bnQuZ292LnVrIiwibmJmIjoxNjQ5MTc1NDU1LCJzaGFyZWRfY2xhaW1zIjp7IkBjb250ZXh0IjpbImh0dHBzOlwvXC93d3cudzMub3JnXC8yMDE4XC9jcmVkZW50aWFsc1wvdjEiLCJodHRwczpcL1wvdm9jYWIubG9uZG9uLmNsb3VkYXBwcy5kaWdpdGFsXC9jb250ZXh0c1wvaWRlbnRpdHktdjEuanNvbmxkIl0sImJpcnRoRGF0ZSI6W3sidmFsdWUiOiIxOTY0LTA5LTAzIn1dLCJuYW1lIjpbeyJuYW1lUGFydHMxMjM0IjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLRU5ORVRIIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiREVDRVJRVUVJUkEifV19XSwiYWRkcmVzc2VzIjpbeyJjdXJyZW50QWRkcmVzcyI6dHJ1ZSwidG93bkNpdHkiOiJCQVRIIiwic3RyZWV0MiI6IkhBRExFWSBST0FEIiwic3RyZWV0MSI6IjgiLCJwb3N0Q29kZSI6IkJBMiA1QUEifV19LCJpc3MiOiJodHRwczpcL1wvZGV2LmNvcmUuaXB2LmFjY291bnQuZ292LnVrIiwiZXhwIjoxNjQ5MTc5MDU1LCJpYXQiOjE2NDkxNzU0NTV9.Oh4Lraq0NBykGkG1QmgJO0uqO2lh-b2fyBIVyEs_pBj5euG2ea-XfOuC6ehkYJGTxiBFxeel7bA_EvZ_7yWFqp1f0ZP-i-N3CMWCOahXTM4W27CpgUzXU5TexFAzE5ikzFGWBb8Rtap1ajhOOALLSynY9sjo2oX18aTSCBM9l20093MMoaTX-vLwIB95scETK31ccV7-VvDkCdhw6ApneWLOyF0pADzP4Zr3cJU298pk6FBAfQch4swDWmLVqVkjo-Zq6CUlLQxRLWVwWlf4sOH39g_Fu_fgKIS_1XKAv-RnkrQpO1v5og2Av8hOKf1cwYSKsUwsxukgCI6AO8pzKFsEYA3aK5LE__WFZu4PFnACzhv5-HzLpS1O80YphchR9LwJUSdlgri9UmFgtVFj7-_Et-W0MGuQ7SQ5mOLEigGxnhGkRSw7nzWvV6hTs16-KAbqGtf1DiBZ4rpIyp30b2stLG2SE9IPFO4pGocfofGmB2B7_v5hUlAcBejekzlxtKFrIBSvZWlkAEkpdO5lTD7L9mpcUiOns1qCZWjEKeoGUMz5D3pKyZS9ceV-cagBRBN7QGTmHnvTDdmOKFiPx_xux-xDMd5LU8GWyl0WZumVDMj9VvQGimSCxi_jhAW2CS5K6_IvUSfQSR17b2fZNhhBGJjqTK-j0X4FwYwr0a0";

    public static final String PERSON_SHARED_ATTRIBUTE =
            "{\n"
                    + "    \"addresses\": [\n"
                    + "      {\n"
                    + "        \"townCity\": \"BATH\",\n"
                    + "        \"street1\": \"8\",\n"
                    + "        \"postCode\": \"BA2 5AA\",\n"
                    + "        \"street2\": \"HADLEY ROAD\",\n"
                    + "        \"currentAddress\": true\n"
                    + "      }\n"
                    + "    ],\n"
                    + "    \"name\": [\n"
                    + "      {\n"
                    + "        \"nameParts\": [\n"
                    + "          {\n"
                    + "            \"type\": \"GivenName\",\n"
                    + "            \"value\": \"KENNETH\"\n"
                    + "          },\n"
                    + "          {\n"
                    + "            \"type\": \"FamilyName\",\n"
                    + "            \"value\": \"DECERQUEIRA\"\n"
                    + "          }\n"
                    + "        ]\n"
                    + "      }\n"
                    + "    ],\n"
                    + "    \"@context\": [\n"
                    + "      \"https://www.w3.org/2018/credentials/v1\",\n"
                    + "      \"https://vocab.london.cloudapps.digital/contexts/identity-v1.jsonld\"\n"
                    + "    ],\n"
                    + "    \"birthDate\": [\n"
                    + "      {\n"
                    + "        \"value\": \"1964-09-03\"\n"
                    + "      }\n"
                    + "    ]\n"
                    + "  }";
}
