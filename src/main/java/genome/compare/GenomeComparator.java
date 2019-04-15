package genome.compare;

import exception.GenomeException;
import genome.assembly.GenomeRegion;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines the class, that compares and analyses the genomes of two people,
 * using the Levenshtein's and Hemming's distances.
 *
 * @author Sergey Hvatov
 * @author Vladislav Marchenko
 */
public class GenomeComparator {

    /**
     * Genome of the first person.
     */
    private List<GenomeRegion> firstPersonGenome_;

    /**
     * Genome of the second person.
     */
    private List<GenomeRegion> secondPersonGenome_;

    /**
     * Default class constructor from genomes of two people.
     *
     * @param first  Genome of the first person.
     * @param second Genome of the second person.
     */
    public GenomeComparator(List<GenomeRegion> first, List<GenomeRegion> second) throws GenomeException {
        this.firstPersonGenome_ = first;
        this.secondPersonGenome_ = second;

        // sort genome regions
        this.firstPersonGenome_.sort(GenomeComparator::compareRegions);
        this.secondPersonGenome_.sort(GenomeComparator::compareRegions);

        // validate the regions
        if (validateRegions()) {
            throw new GenomeException(this.getClass().getName(),
                "GenomeComparator",
                "first, second",
                "failed the validation");
        }
    }

    /**
     * Compares two genomes. Calculates Hemming length.
     *
     * @return The list of {@link GeneComparisonResult} that contain
     * the information about differences between all {@link GenomeRegion}.
     * @throws GenomeException if sizes of strings from GenomeRegions are not equal.
     */
    @Deprecated
    public List<GeneComparisonResult> HemmingDistance() throws GenomeException {
        ArrayList<GeneComparisonResult> result = new ArrayList<>();
        for (int i = 0; i < firstPersonGenome_.size(); i++) {
            result.add(hDistance(firstPersonGenome_.get(i), secondPersonGenome_.get(i)));
        }
        return result;
    }

    /**
     * Optimized version of Needleman–Wunsch algorithm using only two rows of the matrix.
     * It's algorithmic complexity is O(min(N, M)).
     *
     * @return The list of {@link GeneComparisonResult} that contain
     * the information about differences between all {@link GenomeRegion}.
     * @throws GenomeException if exception is thrown in {@link GeneComparisonResult}.
     */
    public List<GeneComparisonResult> LevenshteinDistance() throws GenomeException {
        ArrayList<GeneComparisonResult> result = new ArrayList<>();
        for (int i = 0; i < firstPersonGenome_.size(); i++) {
            result.add(lDistance(firstPersonGenome_.get(i), secondPersonGenome_.get(i)));
        }
        return result;
    }

    /**
     * Basic version of Needleman–Wunsch algorithm using a matrix [M * N].
     * It's algorithmic complexity is O(N * M). This method is used to
     * test the Needleman–Wunsch algorithm.
     *
     * @return The list of {@link GeneComparisonResult} that contain
     * the information about differences between all {@link GenomeRegion}.
     * @throws GenomeException if exception is thrown in {@link GeneComparisonResult}.
     */
    @Deprecated
    public List<Pair<GeneComparisonResult, Integer[][]>> LevenshteinDistanceCanonical() throws GenomeException {
        ArrayList<Pair<GeneComparisonResult, Integer[][]>> result = new ArrayList<>();
        for (int i = 0; i < firstPersonGenome_.size(); i++) {
            String f = firstPersonGenome_.get(i).getNucleotideSequence();
            String s = secondPersonGenome_.get(i).getNucleotideSequence();

            // table for the further usage
            Integer table[][] = new Integer[f.length() + 1][s.length() + 1];

            // initialize table
            for (int j = 0; j < f.length() + 1; j++)
                Arrays.fill(table[j], 0);

            // fill the table
            for (int l = 0; l < f.length() + 1; l++) {
                table[l][0] = l;
            }
            for (int l = 0; l < s.length() + 1; l++) {
                table[0][l] = l;
            }

            for (int l = 1; l < f.length() + 1; l++) {
                for (int k = 1; k < s.length() + 1; k++) {
                    // calculate the minimum between
                    // table[l][k-1] + 1, table[l-1][k] + 1
                    // and table[l - 1][k - 1] + (f.charAt(l) == s.charAt(k)
                    table[l][k] = Math.min(
                        Math.min(table[l][k - 1] + 1, table[l - 1][k] + 1),
                        Math.min(table[l][k - 1] + 1, table[l - 1][k - 1] + (f.charAt(l - 1) == s.charAt(k - 1) ? 0 : 1)));
                }
            }
            result.add(new Pair<>(
                new GeneComparisonResult(
                    firstPersonGenome_.get(i).getChromName(),
                    firstPersonGenome_.get(i).getStart(),
                    table[f.length()][s.length()],
                    Math.max(f.length(), s.length())), table)
            );
        }
        return result;
    }

    /**
     * Validate all input genome regions.
     *
     * @return False, if the sizes of the lists doesn't match
     * or after sorting it appears that some regions doesn't have
     * pair.
     */
    private boolean validateRegions() {
        // check the sizes
        if (firstPersonGenome_.size() != secondPersonGenome_.size()) {
            return true;
        }

        // check if all regions from the first person has their pairs
        // <=> both files are parsed using the same BED file.
        for (int i = 0; i < firstPersonGenome_.size(); i++) {
            if (!firstPersonGenome_.get(i).equals(secondPersonGenome_.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Implementation of the method that calculates the Hemming distance
     * for the HemmingDistance() method.
     *
     * @param first  First genome region.
     * @param second Second genome region.
     * @return The result of the comparison using Hemming distance.
     * @throws GenomeException if sizes of the regions are not equal or if exception
     *                         is thrown in {@link GeneComparisonResult}.
     */
    private GeneComparisonResult hDistance(GenomeRegion first, GenomeRegion second) throws GenomeException {
        String f = first.getNucleotideSequence();
        String s = second.getNucleotideSequence();

        int diff = 0;
        // check the sizes because Hemming distance is calculated only
        // for equal strings
        if (s.length() != f.length()) {
            throw new GenomeException("HemmingDistance", "", "sizes of strings from GenomeRegions are not equal");
        }

        for (int j = 0; j < first.getNucleotideLength(); j++) {
            if (f.charAt(j) != s.charAt(j) && f.charAt(j) != getComplementNucleotide(s.charAt(j))) {
                diff++;
            }
        }
        return new GeneComparisonResult(first.getChromName(), first.getStart(), diff, f.length());
    }

    /**
     * Implementation of the method that calculates the Levenshtein distance
     * for the LevenshteinDistance() method.
     *
     * @param first  First genome region.
     * @param second Second genome region.
     * @return The result of the comparison using Levenshtein distance.
     * @throws GenomeException if sizes of the regions are not equal or if exception
     *                         is thrown in {@link GeneComparisonResult}.
     */
    private GeneComparisonResult lDistance(GenomeRegion first, GenomeRegion second) throws GenomeException {
        String f = first.getNucleotideSequence();
        String s = second.getNucleotideSequence();

        // table for the further usage
        int table[] = new int[s.length() + 1];
        // fill the table
        for (int l = 0; l < s.length() + 1; l++) {
            table[l] = l;
        }

        int current[] = new int[s.length() + 1];
        for (int l = 1; l < f.length() + 1; l++) {
            current[0] = l;
            for (int k = 1; k < s.length() + 1; k++) {
                // calculate the minimum between
                // table[l][k-1] + 1, table[l-1][k] + 1
                // and table[l - 1][k - 1] + (f.charAt(l) == s.charAt(k)
                current[k] = Math.min(
                    Math.min(current[k - 1] + 1, table[k] + 1),
                    Math.min(table[k] + 1, table[k - 1] + (f.charAt(l - 1) == s.charAt(k - 1) ? 0 : 1)));
            }
            table = Arrays.copyOf(current, current.length);
        }
        return new GeneComparisonResult(
            first.getChromName(),
            first.getStart(),
            current[s.length()],
            Math.max(f.length(), s.length())
        );
    }

    /**
     * Get the complement nucleotide for this one method.
     *
     * @param nucleotide
     * @return Complement to this nucleotide.
     */
    private static char getComplementNucleotide(char nucleotide) {
        switch (nucleotide) {
            case 'a':
                return 't';
            case 't':
                return 'a';
            case 'g':
                return 'c';
            case 'c':
                return 'g';
            default:
                return '*';
        }
    }

    /**
     * Compares two genome regions for their further sort.
     *
     * @param lhs Left genome region.
     * @param rhs Right genome region.
     * @return value according to {@link Comparable} rules.
     */
    private static int compareRegions(GenomeRegion lhs, GenomeRegion rhs) {
        Integer lhsChrom = chromToInt(lhs.getChromName());
        Integer rhsChrom = chromToInt(rhs.getChromName());
        if (lhsChrom.compareTo(rhsChrom) != 0) {
            return lhsChrom.compareTo(rhsChrom);
        } else {
            return ((Integer) lhs.getStart()).compareTo(rhs.getStart());
        }
    }

    /**
     * Converts chromosome name to some integer representation.
     *
     * @param chrom Name of the chromosome.
     * @return Sum of multiplication of all chars from the string
     * and 10 ^ (position of the char in the string).
     */
    private static int chromToInt(String chrom) {
        int result = 0;
        char temp[] = (new StringBuilder(chrom)).reverse().toString().toCharArray();
        for (int i = 0; i < temp.length; i++) {
            result += Character.getNumericValue(temp[i]) * Math.pow(10, i);
        }
        return result;
    }
}
