package uk.gov.di.ipv.cri.kbv.api.data;

public final class TestData {
    public static final String APERSONIDENTITY =
            "{\"names\":[{\"surname\":\"DECERQUEIRA\",\"firstName\":\"KENNETH\"}],\"UKAddresses\":[{\"townCity\":\"BATH\",\"street1\":\"8\",\"postCode\":\"BA2 5AA\",\"street2\":\"HADLEY ROAD\",\"currentAddress\":true}],\"datesOfBirth\":[\"1948-09-26\"]}";
    ;
    public static final String GOODJWT =
            "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJlNWE0ODZjOC1jM2VjLTQ1NTYtOTc0NS05NWZjOTQzY2UxZGMiLCJuYmYiOjE2NDc4NzUxMjMsImlzcyI6Imlwdi1jb3JlLXN0dWIiLCJjbGFpbXMiOnsidmNfaHR0cF9hcGkiOnsiZGF0ZXNPZkJpcnRoIjpbIjE5NDgtMDktMjYiXSwibmFtZXMiOlt7ImZpcnN0TmFtZSI6IktFTk5FVEgiLCJzdXJuYW1lIjoiREVDRVJRVUVJUkEifV0sIlVLQWRkcmVzc2VzIjpbeyJjdXJyZW50QWRkcmVzcyI6dHJ1ZSwidG93bkNpdHkiOiJCQVRIIiwic3RyZWV0MiI6IkhBRExFWSBST0FEIiwic3RyZWV0MSI6IjgiLCJwb3N0Q29kZSI6IkJBMiA1QUEifV19fSwiZXhwIjoxNjQ3ODc4NzIzLCJpYXQiOjE2NDc4NzUxMjN9.A5BRNmJTa2pvbvOelDUBRN-4tRppXuJCekz_H_EDrkcqQRBpu2dfmKz6XdLYjNWTaTfg_iKS_xJUBsH8WXHdv2SqUnaofBjfTxURtM-ztwioXTsuKayjK103HHfKdcYgHMJRdgbpdK2EYD9wvxfIHISH9EH4tgJqv2xQ54tT4qpGEaEN0DOkXQhr6RW0a5B5NeQyzckjiODKf-cVJifklZ6VjJNTXGxCKU8v8ouQYAt6AJvSwmPqjpbjbWNRBwFP9xAFwGy5phXnPtzyyvSrviSIqkwIBx6X4-NLr7PJurNXX0HJYD1voPV8S0QlrrFoL0GV_sQn-9MgA13bOJ5mJgisLEuwFFQ3ksjw52FUM_mn0accWIr52nN62U85l550hOr35xvwfCyIdnRbCtythQu-uOpQ2z_wBWfvB-MCppLpxLeX6bdDuoQM3LglS0WnLD7dzxjGcG1f7Z8HQkdDefHtpjfawN2op0oqt3If4FO6Jvhq473D97Pa72tp746h2Lo5ROQKNmqoPaKIsbncVpPXis7D-R9z_ak4IXuoAE-6iABFjGVSfy354ndY9JruoBYiNH0U8XMk_FI4CgzIvhN22qOUgME--hpJdJ4z-vjD0gyPxx0xlioY4caWBVYiMrN7p9e1t86SHw7WpLudeRAGKYs0-hGBolXt_0B-0B0";
    public static final String BADJWT =
            "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJlNWI1MDhiMC0xNDcyLTRmY2YtODYyYS0zNmZlMDFiMTAyYTYiLCJuYmYiOjE2NDc4NzQ3MzQsImlzcyI6Imlwdi1jb3JlLXN0dWIiLCJjbGFpbXMiOnsidmNfaHR0cF9hcGkiOnsibmFtZXMiOlt7InN1cm5hbWUiOiJERUNFUlFVRUlSQSIsImZpcnN0TmFtZTEyMzQiOiJLRU5ORVRIIn1dLCJkYXRlc09mQmlydGgiOlsiMTk0OC0wOS0yNiJdLCJVS0FkZHJlc3NlcyI6W3sic3RyZWV0MSI6IjgiLCJzdHJlZXQyIjoiSEFETEVZIFJPQUQiLCJ0b3duQ2l0eSI6IkJBVEgiLCJjdXJyZW50QWRkcmVzcyI6dHJ1ZSwicG9zdENvZGUiOiJCQTIgNUFBIn1dfX0sImV4cCI6MTY0Nzg3ODMzNCwiaWF0IjoxNjQ3ODc0NzM0fQ.aVSD7v-cTCC2MmO3rbfORqrBs6z6OT-UIPyxexkgfln1qLbHiCch1FvGXpKONqxiX6MhfevZA3hdQQVy6--MsXACKubQy9HxLNHK8wJx9LB7r9woLS4QizG0niLr6YkFbF1ARQimBBGjmhb9KjUSYU3Yn-lD_naXfwvnr4FpS9Uy9MTpaSspdW4mzen5iJuEOornyVb7IXo9J4UJAkDM2KeDvYJIcHA_mzjrdxyADT8alYp0Z2yZ-sXdGbG5kZ7ADZ4yKlMubMjnNlqmir0LE5vklYE9b9qwJIwOTkA60tBvOCm7hGtdVrjVBO5LaCoVre1xzaE9HC_a9xfzHtWx9R-pVx5nyww4_7s48F7Jtd25DtOaoif-5otpTmh8Xu5sVLQ2lwpR-57Q0Nbcwq4JvgbTgEhJUk6G2SrCNvoy3wTrOJ9iPOR-r_3WVl-WbRCtNurjO2f2PQNZVLgQPygQSSwPwuD-tb7ah7brbisf6fBxid6ceBftGyIMvUwpYbVNB6EG3HjPQdT-XYZQWVQSmL2Lzck7-75b4onxP6jvIx92wQiZ3cXs27VZfpFCKNZPj36PcQIPnLnHn-5q8nM6VOCrPxZsUp-rKjtflWzK92kkzxLMMOFlLrfW3XlG6YANju3si4vcIQGL3cCzVQ0ocCd9xLKGHnEzGv-OznQ6dAA";

    public static final String PERSON_SHARED_ATTRIBUTE = "{\n" +
            "    \"datesOfBirth\": [\n" +
            "        \"1948-09-26\"\n" +
            "    ],\n" +
            "    \"names\": [\n" +
            "        {\n" +
            "            \"firstName\": \"KENNETH\",\n" +
            "            \"surname\": \"DECERQUEIRA\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"UKAddresses\": [\n" +
            "        {\n" +
            "            \"currentAddress\": true,\n" +
            "            \"townCity\": \"BATH\",\n" +
            "            \"street2\": \"HADLEY ROAD\",\n" +
            "            \"street1\": \"8\",\n" +
            "            \"postCode\": \"BA2 5AA\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";
}
