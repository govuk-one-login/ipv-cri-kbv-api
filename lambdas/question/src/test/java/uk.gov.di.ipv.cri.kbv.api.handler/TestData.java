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
    public static final String INITIAL_QUESTION_STATE = "{\"control\":null,\"qaPairs\":[],\"nextQuestion\":{\"empty\":true,\"present\":false}}";

    public static  final String expectedResult = "{\n" +
            "    \"control\": {\n" +
            "        \"urn\": \"77597552-19e4-4a49-a350-787506f14964\",\n" +
            "        \"authRefNo\": \"7CBSZJC44B\",\n" +
            "        \"dateTime\": null,\n" +
            "        \"testDatabase\": \"A\",\n" +
            "        \"clientAccountNo\": \"J8193\",\n" +
            "        \"clientBranchNo\": null,\n" +
            "        \"operatorID\": \"GDSCABINETUIIQ01U\",\n" +
            "        \"parameters\": {\n" +
            "            \"oneShotAuthentication\": \"N\",\n" +
            "            \"storeCaseData\": \"P\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"questions\": {\n" +
            "        \"question\": [\n" +
            "            {\n" +
            "                \"questionID\": \"Q00036\",\n" +
            "                \"text\": \"In which year did you open your loan?\",\n" +
            "                \"tooltip\": \"Your current, active, personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\n" +
            "                \"answerFormat\": {\n" +
            "                    \"identifier\": \"A00004\",\n" +
            "                    \"fieldType\": \"G \",\n" +
            "                    \"answerList\": [\n" +
            "                        \"2022\",\n" +
            "                        \"2016\",\n" +
            "                        \"2017\",\n" +
            "                        \"2015\",\n" +
            "                        \"NONE OF THE ABOVE / DOES NOT APPLY\"\n" +
            "                    ]\n" +
            "                },\n" +
            "                \"answerHeldFlag\": null\n" +
            "            },\n" +
            "            {\n" +
            "                \"questionID\": \"Q00042\",\n" +
            "                \"text\": \"How much is your monthly loan payment?\",\n" +
            "                \"tooltip\": \"The approximate amount, in £s, that you should pay each month, not the amount that you actually pay. Personal Loans only, does not include HP Loans, 2nd Mortgages or Home Credit.\",\n" +
            "                \"answerFormat\": {\n" +
            "                    \"identifier\": \"A00004\",\n" +
            "                    \"fieldType\": \"G \",\n" +
            "                    \"answerList\": [\n" +
            "                        \"UP TO £400\",\n" +
            "                        \"OVER £400 UP TO £450\",\n" +
            "                        \"OVER £450 UP TO £500\",\n" +
            "                        \"OVER £500 UP TO £550\",\n" +
            "                        \"NONE OF THE ABOVE / DOES NOT APPLY\"\n" +
            "                    ]\n" +
            "                },\n" +
            "                \"answerHeldFlag\": null\n" +
            "            }\n" +
            "        ],\n" +
            "        \"skipsRemaining\": null,\n" +
            "        \"skipWarning\": null\n" +
            "    },\n" +
            "    \"results\": {\n" +
            "        \"outcome\": \"Authentication Questions returned\",\n" +
            "        \"authenticationResult\": null,\n" +
            "        \"questions\": null,\n" +
            "        \"alerts\": null,\n" +
            "        \"nextTransId\": {\n" +
            "            \"string\": [\n" +
            "                \"RTQ\"\n" +
            "            ]\n" +
            "        },\n" +
            "        \"caseFoundFlag\": null,\n" +
            "        \"confirmationCode\": null\n" +
            "    },\n" +
            "    \"error\": null\n" +
            "}";

    public static final String QUESTION_STATE = "{\"control\":{\"authRefNo\":\"7CASCCWAN9\",\"urn\":\"56f62b11-8bcc-4836-a705-aed23d2ad97a\",\"dateTime\":null,\"testDatabase\":\"A\",\"clientAccountNo\":\"J8193\",\"clientBranchNo\":null,\"operatorID\":\"GDSCABINETUIIQ01U\",\"parameters\":{\"oneShotAuthentication\":\"N\",\"storeCaseData\":\"P\"}},\"qaPairs\":[{\"question\":{\"questionID\":\"Q00015\",\"text\":\"What is the outstanding balance of your current mortgage?\",\"tooltip\":\"The approximate amount in £s, including interest. A loan to buy property (or land) where the loan is secured by a charge on that property.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £60,000\",\"OVER £60,000 UP TO £85,000\",\"OVER £85,000 UP TO £110,000\",\"OVER £110,000 UP TO £135,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":null},{\"question\":{\"questionID\":\"Q00018\",\"text\":\"How much is your  contracted monthly mortgage payment?\",\"tooltip\":\"The approximate amount, in £s, that you should pay each month, not the amount that you actually pay. This amount could have changed since the start of the mortgage. A loan to buy property (or land) where the loan is secured by a charge on that property.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £600\",\"OVER £600 UP TO £700\",\"OVER £700 UP TO £800\",\"OVER £800 UP TO £900\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}},\"answer\":null}],\"nextQuestion\":{\"empty\":false,\"present\":true}}";

    public static final String EXPECTED_QUESTION = "{\"questionID\":\"Q00015\",\"text\":\"What is the outstanding balance of your current mortgage?\",\"tooltip\":\"The approximate amount in £s, including interest. A loan to buy property (or land) where the loan is secured by a charge on that property.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £60,000\",\"OVER £60,000 UP TO £85,000\",\"OVER £85,000 UP TO £110,000\",\"OVER £110,000 UP TO £135,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}}";
}
