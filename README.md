# Generic Java Recommender System Engine ⚙️📊

A robust, scalable, and fully generic Recommender System built purely in Java. This project was developed to demonstrate advanced Object-Oriented Design (OOD) principles, heavy utilization of the Java Streams API for functional data processing, and deep architectural thinking.

The engine processes datasets of users, items, and ratings, and generates personalized Top-N recommendation lists using multiple interchangeable algorithms.

## 💎 Technical Highlights & Skills Showcased

### 1. Advanced Generics (`<T extends Item>`)
The entire architecture is strictly domain-agnostic. By leveraging Java Generics with bounded type parameters, the engine ensures absolute compile-time type safety. Whether the dataset consists of `Movie`, `Book`, or `Product` objects, as long as they implement the base `Item` interface, the algorithms adapt automatically without requiring a single cast or code duplication.

### 2. Functional Programming (Java Streams API)
Traditional imperative `for`-loops were intentionally avoided in favor of the declarative power of the **Java Streams API**. The data processing pipeline highlights:
* **Complex Aggregations:** Utilizing `Collectors.groupingBy` and `Collectors.averagingDouble` to compute item popularity and user biases dynamically.
* **Filtering & Mapping:** Elegant, chainable stream operations to extract specific user neighborhoods and filter out statistically insignificant data (e.g., the "cold-start" threshold).
* **Sorting & Ranking:** Seamless integration of `Comparator.comparingDouble` within streams to efficiently rank and extract the Top-10 recommendations.

### 3. Deep OOP Architecture (Abstraction & Polymorphism)
The system is built from the ground up to be modular and highly extensible:
* **Abstraction & Inheritance:** The core engine is built around an abstract `RecommenderSystem<T>` class. This parent class encapsulates all the heavy lifting—data indexing, state management, and baseline calculations—allowing the specific algorithm subclasses to focus purely on their unique mathematical models.
* **Polymorphism:** The system acts as a Strategy Pattern. The underlying recommendation algorithm can be dynamically swapped out at runtime while exposing the same clean, high-level API (`recommendTop10(userId)`).

### 4. Optimized Data Structures & Performance
A recommendation engine requires constant, rapid cross-referencing. To optimize performance, the system pre-computes and indexes the dataset upon initialization:
* Constructs multi-dimensional maps (e.g., `Map<Integer, List<Rating>>` for `ratingsByItem` and `ratingsByUser`) to guarantee **O(1) access times** during the active recommendation phase.
* Pre-calculates and caches the global average and individual biases, drastically reducing the computational complexity of the Collaborative Filtering matrix operations.

## 🏗️ Architecture (UML)

```mermaid
classDiagram
    class RecommenderSystem~T~ {
        <<abstract>>
        #Map users
        #Map items
        #List ratings
        #Map ratingsByItem
        #Map ratingsByUser
        #double globalBias
        +recommendTop10(int userId) List~T~
        #getItemsIdsRatedByUser(int userId) Set~Integer~
    }
    
    class PopularityBasedRecommender~T~ {
        +recommendTop10(int userId) List~T~
        +getItemAverageRating(int itemId) double
        +getItemRatingsCount(int itemId) int
    }
    
    class ProfileBasedRecommender~T~ {
        +recommendTop10(int userId) List~T~
        +getMatchingProfileUsers(int userId) List~User~
    }
    
    class SimilarityBasedRecommender~T~ {
        -Map itemBiases
        -Map userBiases
        +recommendTop10(int userId) List~T~
        +getSimilarity(int u1, int u2) double
    }

    RecommenderSystem <|-- PopularityBasedRecommender
    RecommenderSystem <|-- ProfileBasedRecommender
    RecommenderSystem <|-- SimilarityBasedRecommender
