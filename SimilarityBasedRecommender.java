import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Collaborative-filtering recommender with bias correction.
 *
 * Each raw rating is decomposed into:
 *   rating = globalBias + itemBias + userBias + interactionTerm
 *
 * Similarity between two users is the dot product of their bias-free ratings
 * on items they have both rated (minimum 10 shared items required).
 * The top-10 most similar neighbours then vote on unseen items via a
 * weighted average, and biases are added back to produce the final prediction.
 */
class SimilarityBasedRecommender<T extends Item> extends RecommenderSystem<T> {

    private static final int MIN_SHARED_ITEMS   = 10;
    private static final int MIN_NEIGHBOR_VOTES = 5;
    private static final int TOP_NEIGHBORS      = 10;

    private final Map<Integer, Double> itemBiases;
    private final Map<Integer, Double> userBiases;

    public SimilarityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);

        // Item bias: how much an item's average deviates from the global mean
        this.itemBiases = items.keySet().stream().collect(toMap(
                id -> id,
                id -> ratingsByItem.getOrDefault(id, List.of()).stream()
                        .mapToDouble(r -> r.getRating() - globalBias)
                        .average().orElse(0.0)
        ));

        // User bias: personal tendency to rate above/below the adjusted mean
        this.userBiases = users.keySet().stream().collect(toMap(
                id -> id,
                id -> ratingsByUser.getOrDefault(id, List.of()).stream()
                        .mapToDouble(r -> r.getRating() - globalBias
                                - itemBiases.getOrDefault(r.getItemId(), 0.0))
                        .average().orElse(0.0)
        ));
    }

    /** Strips global, item and user biases from a raw rating. */
    private double biasFreeRating(Rating<T> r) {
        return r.getRating()
                - globalBias
                - itemBiases.getOrDefault(r.getItemId(), 0.0)
                - userBiases.getOrDefault(r.getUserId(), 0.0);
    }

    /**
     * Cosine-style similarity: dot product of bias-free rating vectors.
     * Returns 0 if the users share fewer than MIN_SHARED_ITEMS rated items,
     * or if u1 == u2.
     */
    public double getSimilarity(int u1, int u2) {
        if (u1 == u2) return 0;

        List<Rating<T>> ratingsU1 = ratingsByUser.getOrDefault(u1, List.of());
        List<Rating<T>> ratingsU2 = ratingsByUser.getOrDefault(u2, List.of());

        Map<Integer, Rating<T>> u2ByItem = ratingsU2.stream()
                .collect(toMap(Rating::getItemId, r -> r));

        List<Rating<T>> shared = ratingsU1.stream()
                .filter(r -> u2ByItem.containsKey(r.getItemId()))
                .collect(toList());

        if (shared.size() < MIN_SHARED_ITEMS) return 0;

        return shared.stream()
                .mapToDouble(r -> biasFreeRating(r) * biasFreeRating(u2ByItem.get(r.getItemId())))
                .sum();
    }

    @Override
    public List<T> recommendTop10(int userId) {
        // Step 1: find the TOP_NEIGHBORS most similar users
        List<Integer> neighbors = users.keySet().stream()
                .filter(id -> id != userId)
                .map(id -> new AbstractMap.SimpleEntry<>(id, getSimilarity(userId, id)))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(TOP_NEIGHBORS)
                .map(Map.Entry::getKey)
                .collect(toList());

        Set<Integer> alreadyRated = getItemsIdsRatedByUser(userId);

        // Step 2: keep only items rated by at least MIN_NEIGHBOR_VOTES neighbours
        return items.keySet().stream()
                .filter(itemId -> !alreadyRated.contains(itemId))
                .filter(itemId -> neighbors.stream()
                        .filter(nId -> ratingsByUser.getOrDefault(nId, List.of()).stream()
                                .anyMatch(r -> r.getItemId() == itemId))
                        .count() >= MIN_NEIGHBOR_VOTES)
                // Step 3: sort by predicted rating, then total rating count, then name
                .map(items::get)
                .sorted(Comparator
                        .comparingDouble((T item) -> predictRating(userId, item.getId(), neighbors)).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (T item) -> ratingsByItem.getOrDefault(item.getId(), List.of()).size()).reversed())
                        .thenComparing(Item::getName))
                .limit(NUM_OF_RECOMMENDATIONS)
                .collect(toList());
    }

    /**
     * Weighted average of neighbours' bias-free ratings, with biases added back.
     * prediction = globalBias + itemBias + userBias + Σ(sim * bfRating) / Σ(sim)
     */
    private double predictRating(int userId, int itemId, List<Integer> neighbors) {
        double weightedSum = neighbors.stream()
                .mapToDouble(nId -> {
                    double sim = getSimilarity(userId, nId);
                    return ratingsByUser.getOrDefault(nId, List.of()).stream()
                            .filter(r -> r.getItemId() == itemId)
                            .findFirst()
                            .map(r -> sim * biasFreeRating(r))
                            .orElse(0.0);
                }).sum();

        double simSum = neighbors.stream()
                .filter(nId -> ratingsByUser.getOrDefault(nId, List.of()).stream()
                        .anyMatch(r -> r.getItemId() == itemId))
                .mapToDouble(nId -> getSimilarity(userId, nId))
                .sum();

        double interaction = (simSum == 0) ? 0 : weightedSum / simSum;

        return globalBias
                + itemBiases.getOrDefault(itemId, 0.0)
                + userBiases.getOrDefault(userId, 0.0)
                + interaction;
    }

    public void printGlobalBias() {
        System.out.printf("Global bias: %.2f%n", globalBias);
    }

    public void printItemBias(int itemId) {
        System.out.printf("Item bias for item %d: %.2f%n", itemId, itemBiases.getOrDefault(itemId, 0.0));
    }

    public void printUserBias(int userId) {
        System.out.printf("User bias for user %d: %.2f%n", userId, userBiases.getOrDefault(userId, 0.0));
    }
}
