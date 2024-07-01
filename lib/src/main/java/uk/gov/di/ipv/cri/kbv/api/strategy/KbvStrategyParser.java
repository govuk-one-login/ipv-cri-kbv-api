package uk.gov.di.ipv.cri.kbv.api.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class KbvStrategyParser {
    private final String strategy;

    public KbvStrategyParser(String strategy) {
        this.strategy = strategy;
    }

    public Strategy parse() {
        List<Integer> numbers =
                Arrays.stream(strategy.split("\\D+"))
                        .filter(Predicate.not(String::isEmpty))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());

        if (numbers.size() != 2) {
            throw new IllegalArgumentException(String.format("Invalid input string: %s", strategy));
        }

        int first = numbers.get(0);
        int second = numbers.get(1);

        if (first >= second) {
            throw new IllegalArgumentException(
                    String.format(
                            "First number %d must be less than second number %d: %s",
                            first, second, strategy));
        }
        return new Strategy(first, second);
    }
}
