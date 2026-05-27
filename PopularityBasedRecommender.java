import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * Recommends items that are broadly popular — high average rating with enough
 * votes to be statistically meaningful (at least POPULARITY_THRESHOLD ratings).
 */
class PopularityBasedRecommender<T extends Item> extends RecommenderSystem<T> {

    private static final int POPULARITY_THRESHOLD = 100;

    public PopularityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    @Override
    public List<T> recommendTop10(int userId) {
        Set<Integer> alreadyRated = getItemsIdsRatedByUser(userId);

        return items.values().stream()
                .filter(item -> getItemRatingsCount(item.getId()) >= POPULARITY_THRESHOLD)
                .filter(item -> !alreadyRated.contains(item.getId()))
                .sorted(Comparator
                        .comparingDouble((T item) -> getItemAverageRating(item.getId())).reversed()
                        .thenComparing(Comparator.comparingInt((T item) -> getItemRatingsCount(item.getId())).reversed())
                        .thenComparing(Item::getName))
                .limit(NUM_OF_RECOMMENDATIONS)
                .collect(toList());
    }

    /** Average rating for an item; returns 0.0 if the item has no ratings. */
    public double getItemAverageRating(int itemId) {
        List<Rating<T>> itemRatings = ratingsByItem.get(itemId);
        if (itemRatings == null || itemRatings.isEmpty()) return 0.0;
        return itemRatings.stream()
                .mapToInt(Rating::getRating)
                .average()
                .orElse(0.0);
    }

    /** Total number of ratings for an item. */
    public int getItemRatingsCount(int itemId) {
        List<Rating<T>> itemRatings = ratingsByItem.get(itemId);
        return (itemRatings == null) ? 0 : itemRatings.size();
    }
}
