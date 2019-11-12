package lol.karl.clueless;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class Clueless {

    private Dictionary dictionary;

    public Clueless(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * @param clues         all the incoming clues in the puzzle
     * @param solvedLetters the starter clues, which will be populated
     * @param finalPuzzle   the last "clue" to solve
     * @param guessDepth    the number of guesses we are relying on at this point
     * @return the answer to the puzzle, or null if there are no solutions with the given inputs
     */
    public String solve(List<Clue> clues, String[] solvedLetters, Clue finalPuzzle, int guessDepth) {
        findSureAnswers(clues, solvedLetters);

        // Only try and solve the final answer if there are currently no guesses or if all the clues are successfully used
        if (guessDepth == 0 || clues.isEmpty()) {
            log.debug("Attempting to solve final answer");
            finalPuzzle.updatePatternAndMatchingWords(solvedLetters, dictionary);
            if (finalPuzzle.hasSureAnswer()) {
                log.debug("Final clue {} can only be {}", finalPuzzle.pattern.pattern(), finalPuzzle.possibleAnswers.get(0));
                return finalPuzzle.possibleAnswers.get(0);
            }
        }

        if (!clues.isEmpty()) {
            // Find clue with least alternatives and remove from list of clues
            clues.sort(Comparator.comparingInt(clue -> clue.numPossibleAnswers));
            Clue clueWithLeastAlternatives = clues.get(0);

            if (clueWithLeastAlternatives.numPossibleAnswers == 0) {
                log.debug("Clue {} has no solution!", clueWithLeastAlternatives.pattern);
                return null;
            }

            clues.remove(clueWithLeastAlternatives);

            // Attempt to solve using each possible alternative
            for (String possibleAnswer : clueWithLeastAlternatives.possibleAnswers) {
                log.debug("Branching out, trying {}, out of {}", possibleAnswer, clueWithLeastAlternatives.possibleAnswers);
                String[] prospectiveSolvedLetters = createNewSolvedLettersWithAnswer(clueWithLeastAlternatives, possibleAnswer, solvedLetters);
                String finalAnswer = solve(clues, prospectiveSolvedLetters, finalPuzzle, guessDepth + 1);
                if (finalAnswer != null) {
                    return finalAnswer;
                }
            }

            log.debug("Backtracking, re-adding {}", clueWithLeastAlternatives.pattern);
            clues.add(clueWithLeastAlternatives);
        }

        return null;
    }

    /**
     * Keeps iterating to find sure answers until there are no more sure answers
     */
    private void findSureAnswers(List<Clue> clues, String[] solvedLetters) {
        boolean newLetterDiscovered = true;
        while (newLetterDiscovered) {
            // Update the state of the clues with the latest known info
            for (Clue clue : clues) {
                clue.updatePatternAndMatchingWords(solvedLetters, dictionary);
            }

            log.debug("Scanning {} clues for sure answers", clues.size());
            newLetterDiscovered = false;
            Iterator<Clue> clueIterator = clues.iterator();
            while (clueIterator.hasNext()) {
                Clue clue = clueIterator.next();

                if (clue.hasSureAnswer()) {
                    log.debug("Clue {} can only be {}", clue.pattern.pattern(), clue.possibleAnswers.get(0));
                    updateSolvedLettersWithAnswer(clue, clue.possibleAnswers.get(0), solvedLetters);
                    newLetterDiscovered = true;
                    clueIterator.remove();
                }
            }
        }
        log.debug("No more sure answers");
    }

    private String[] createNewSolvedLettersWithAnswer(Clue clue, String answer, String[] solvedLetters) {
        String[] newSolvedLetters = Arrays.copyOf(solvedLetters, solvedLetters.length);
        updateSolvedLettersWithAnswer(clue, answer, newSolvedLetters);
        return newSolvedLetters;
    }

    private void updateSolvedLettersWithAnswer(Clue clue, String answer, String[] solvedLetters) {
        for (int i = 0; i < clue.digits.length; i++) {
            int clueNumber = clue.digits[i];
            if (solvedLetters[clueNumber] == null) {
                char discoveredLetter = answer.charAt(i);
                log.debug("  Solved {} = {}", clueNumber, discoveredLetter);
                solvedLetters[clueNumber] = String.valueOf(discoveredLetter);
            }
        }
    }
}
