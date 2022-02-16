package uk.gov.di.ipv.cri.kbv.api.handler;

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
    public static final String EXPECTED_QUESTION_STATE = "{\"control\":{\"authRefNo\":\"7CASD7WA7B\",\"urn\":\"50caf0fa-84f3-44af-98c8-5ca88460cb83\",\"dateTime\":null,\"testDatabase\":\"A\",\"clientAccountNo\":\"J8193\",\"clientBranchNo\":null,\"operatorID\":\"GDSCABINETUIIQ01U\",\"parameters\":{\"oneShotAuthentication\":\"N\",\"storeCaseData\":\"P\"}},\"qaPairs\":[{\"question\":{\"questionID\":\"Q00039\",\"text\":\"What is the balance, including interest, of your  loan?\",\"tooltip\":\"The approximate amount in £s on a current active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £6,250\",\"OVER £6,250 UP TO £6,500\",\"OVER £6,500 UP TO £6,750\",\"OVER £6,750 UP TO £7,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":null},{\"question\":{\"questionID\":\"Q00042\",\"text\":\"How much is your monthly loan payment?\",\"tooltip\":\"The approximate amount, in £s, that you should pay each month, not the amount that you actually pay. Personal Loans only, does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £500\",\"OVER £500 UP TO £550\",\"OVER £550 UP TO £600\",\"OVER £600 UP TO £650\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":null}],\"nextQuestion\":{\"empty\":false,\"present\":true}}";

}
