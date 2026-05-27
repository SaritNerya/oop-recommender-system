import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Recommends items based on what demographically similar users liked.
 * "Similar" is defined as same gender and age within a 5-year window.
 * Only items rated by at least 5 matching users are considered.
 */
class ProfileBasedRecommender<T extends Item> extends RecommenderSystem<T> {

    private static final int MIN_GROUP_RATINGS = 5;

    public ProfileBasedRecommender(Map<Integer, User> users,
                                   Map<Integer, T> items,
                                   List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    @Override
    public List<T> recommendTop10(int userId) {
        Set<Integer> matchingUserIds = getMatchingProfileUsers(userId).stream()
                .map(User::getId)
                .collect(toSet());

        // Build a ratings index scoped to the matching demographic group
        Map<Integer, List<Rating<T>>> groupRatingsByItem = ratings.stream()
                .filter(r -> matchingUserIds.contains(r.getUserId()))
                .collect(groupingBy(Rating::getItemId));

        Set<Integer> alreadyRated = getItemsIdsRatedByUser(userId);

        return items.values().stream()
                .filter(item -> !alreadyRated.contains(item.getId()))
                .filter(item -> groupRatingsByItem.getOrDefault(item.getId(), List.of()).size() >= MIN_GROUP_RATINGS)
                .sorted(Comparator
                        .comparingDouble((T item) -> averageRating(groupRatingsByItem.get(item.getId()))).reversed()
                        .thenComparing(Comparator.comparingInt((T item) -> groupRatingsByItem.get(item.getId()).size()).reversed())
                        .thenComparing(Item::getName))
                .limit(NUM_OF_RECOMMENDATIONS)
                .collect(toList());
    }

    /**
     * Returns users who share the same gender and are within 5 years of age
     * — the "demographic neighbourhood" of the given user.
     */
    public List<User> getMatchingProfileUsers(int userId) {
        User currentUser = users.get(userId);
        if (currentUser == null) return List.of();

        return users.values().stream()
                .filter(u -> u.getId() != userId)
                .filter(u -> u.getGender().equals(currentUser.getGender()))
                .filter(u -> Math.abs(u.getAge() - currentUser.getAge()) <= 5)
                .collect(toList());
    }

    private double averageRating(List<Rating<T>> groupRatings) {
        if (groupRatings == null) return 0.0;
        return groupRatings.stream()
                .mapToInt(Rating::getRating)
                .average()
                .orElse(0.0);
    }
}
