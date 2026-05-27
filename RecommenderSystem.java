import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

/** Abstract generic recommender system. */
abstract class RecommenderSystem<T extends Item> {
    protected final Map<Integer, User> users;
    protected final Map<Integer, T> items;
    protected final List<Rating<T>> ratings;

    /** Pre-built indexes for O(1) lookup instead of scanning the full ratings list each time. */
    protected final Map<Integer, List<Rating<T>>> ratingsByItem;
    protected final Map<Integer, List<Rating<T>>> ratingsByUser;
    protected final double globalBias;

    protected final int NUM_OF_RECOMMENDATIONS = 10;

    protected RecommenderSystem(Map<Integer, User> users,
                                Map<Integer, T> items,
                                List<Rating<T>> ratings) {
        this.users = users;
        this.items = items;
        this.ratings = ratings;

        this.ratingsByItem = ratings.stream().collect(groupingBy(Rating::getItemId));
        this.ratingsByUser = ratings.stream().collect(groupingBy(Rating::getUserId));
        this.globalBias   = ratings.stream().mapToInt(Rating::getRating).average().orElse(0.0);
    }

    /** Returns top-10 recommended items for the given user, sorted best-first. */
    public abstract List<T> recommendTop10(int userId);

    /** Returns the set of item IDs already rated by a user — used to avoid re-recommending. */
    protected Set<Integer> getItemsIdsRatedByUser(int userId) {
        List<Rating<T>> userRatings = ratingsByUser.get(userId);
        if (userRatings == null) return Set.of();
        return userRatings.stream()
                .map(Rating::getItemId)
                .collect(toSet());
    }
}
