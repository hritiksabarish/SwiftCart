package com.grocerystore.config;

import com.grocerystore.entity.*;
import com.grocerystore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * DataInitializer — seeds the SwiftCart database with demo data on first run.
 * Idempotent: skips if products already exist.
 *
 *   Admin login  : admin@swiftcart.com / admin123
 *   Customer login: rahul@example.com  / customer123
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository        userRepository;
    private final CategoryRepository    categoryRepository;
    private final ProductRepository     productRepository;
    private final AddressRepository     addressRepository;
    private final OrderRepository       orderRepository;
    private final OrderItemRepository   orderItemRepository;
    private final ReviewRepository      reviewRepository;
    private final PasswordEncoder       passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        if (productRepository.count() > 0) {
            log.info("SwiftCart ✔ Data already exists — skipping initialization.");
            return;
        }

        log.info("SwiftCart › Starting sample data initialization...");

        // ══════════════════════════════════════════════════════════════════════
        //  USERS
        // ══════════════════════════════════════════════════════════════════════

        User admin = userRepository.save(Objects.requireNonNull(User.builder()
                .name("Admin User")
                .email("admin@swiftcart.com")
                .password(passwordEncoder.encode("admin123"))
                .phone("9999999999")
                .role(User.Role.ADMIN)
                .enabled(true)
                .build()));

        User rahul = userRepository.save(Objects.requireNonNull(User.builder()
                .name("Rahul Sharma")
                .email("rahul@example.com")
                .password(passwordEncoder.encode("customer123"))
                .phone("9876543210")
                .role(User.Role.CUSTOMER)
                .enabled(true)
                .build()));

        log.info("SwiftCart › 2 users created.");

        // ══════════════════════════════════════════════════════════════════════
        //  CATEGORIES (8)
        // ══════════════════════════════════════════════════════════════════════

        Category catFruits    = cat("Fruits & Vegetables", "Fresh farm-direct produce",       "Fruits");
        Category catDairy     = cat("Dairy & Eggs",        "Milk, cheese, butter, eggs",      "Dairy");
        Category catBakery    = cat("Bakery",              "Breads, cakes and baked goods",    "Bakery");
        Category catBeverage  = cat("Beverages",           "Juices, sodas, water, tea",        "Beverages");
        Category catSnacks    = cat("Snacks & Chips",      "Namkeen, biscuits, chocolates",    "Snacks");
        Category catHousehold = cat("Household",           "Cleaning and home care supplies",  "Household");
        Category catPersonal  = cat("Personal Care",       "Soaps, shampoos and skincare",     "Personal+Care");
        Category catMeat      = cat("Meat & Seafood",      "Fresh meat, poultry and fish",     "Meat");

        log.info("SwiftCart › 8 categories created.");

        // ══════════════════════════════════════════════════════════════════════
        //  PRODUCTS — Fruits & Vegetables (5)
        // ══════════════════════════════════════════════════════════════════════

        Product bananas = prod("Fresh Bananas",
                "Premium Cavendish variety, naturally ripened. Twelve per pack.",
                catFruits, "FarmFresh", "dozen", 49, 39, 150);

        Product apples = prod("Red Apples",
                "Crunchy Shimla apples from Himachal Pradesh. Sold per kg.",
                catFruits, "Himalayan", "kg", 180, 149, 80);

        prod("Tomatoes",
                "Fresh ripe vine tomatoes, ideal for cooking and salads. Per kg.",
                catFruits, "FarmFresh", "kg", 40, 35, 200);

        prod("Spinach",
                "Tender organic baby spinach leaves, pesticide-free. Per bunch.",
                catFruits, "Organic+", "bunch", 30, 25, 100);

        prod("Carrots",
                "Fresh orange carrots, crunchy and naturally sweet. Per kg.",
                catFruits, "FarmFresh", "kg", 45, 38, 120);

        // ══════════════════════════════════════════════════════════════════════
        //  PRODUCTS — Dairy & Eggs (4)
        // ══════════════════════════════════════════════════════════════════════

        Product milk = prod("Full Cream Milk",
                "Pasteurized full-cream toned milk, rich in calcium and protein. 1 litre.",
                catDairy, "Amul", "litre", 68, 65, 300);

        prod("Paneer",
                "Fresh soft cottage cheese made from whole milk. 200g pack.",
                catDairy, "Amul", "200g pack", 85, 79, 90);

        prod("Curd",
                "Probiotic dahi with live active cultures for gut health. 400g.",
                catDairy, "Mother Dairy", "400g", 45, 42, 150);

        Product eggs = prod("Eggs",
                "Farm-fresh brown eggs from free-range hens. Pack of 12.",
                catDairy, "Nandhini", "12 pack", 89, 79, 200);

        // ══════════════════════════════════════════════════════════════════════
        //  PRODUCTS — Bakery (3)
        // ══════════════════════════════════════════════════════════════════════

        prod("White Bread",
                "Soft fluffy sandwich bread with a golden crust. 400g loaf.",
                catBakery, "Britannia", "400g loaf", 45, 40, 100);

        prod("Whole Wheat Bread",
                "Multigrain brown bread rich in fibre and nutrients. 400g loaf.",
                catBakery, "Harvest Gold", "400g loaf", 55, 49, 80);

        prod("Butter Croissant",
                "Light flaky golden croissant baked fresh each morning. Per piece.",
                catBakery, "FreshBake", "piece", 35, 30, 60);

        // ══════════════════════════════════════════════════════════════════════
        //  PRODUCTS — Beverages (4)
        // ══════════════════════════════════════════════════════════════════════

        Product orangeJuice = prod("Orange Juice",
                "100% fresh-squeezed orange juice, no preservatives. 1 litre.",
                catBeverage, "Tropicana", "litre", 120, 99, 80);

        prod("Mineral Water",
                "Purified drinking water with essential minerals. 1 litre bottle.",
                catBeverage, "Bisleri", "litre", 20, 18, 500);

        prod("Green Tea",
                "Tulsi-infused green tea bags for immunity and relaxation. 25 bags.",
                catBeverage, "Lipton", "25 bags", 99, 85, 120);

        prod("Mango Drink",
                "Alphonso mango nectar with real fruit pulp. 200 ml pack.",
                catBeverage, "Maaza", "200ml", 30, 25, 200);

        // ══════════════════════════════════════════════════════════════════════
        //  PRODUCTS — Snacks & Chips (4)
        // ══════════════════════════════════════════════════════════════════════

        Product chips = prod("Potato Chips",
                "Classic lightly-salted crispy wafers from fresh potatoes. 100g bag.",
                catSnacks, "Lays", "100g", 30, 28, 250);

        prod("Mixed Namkeen",
                "Crunchy Bombay mix with peanuts, sev and boondi. 400g bag.",
                catSnacks, "Haldirams", "400g", 85, 75, 180);

        prod("Dark Chocolate",
                "Rich 70% cocoa single-origin dark chocolate bar. 80g.",
                catSnacks, "Bournville", "80g bar", 150, 129, 100);

        prod("Digestive Biscuits",
                "Wholegrain wheat digestive biscuits, lightly sweetened. 250g pack.",
                catSnacks, "McVities", "250g pack", 55, 49, 200);

        // ══════════════════════════════════════════════════════════════════════
        //  PRODUCTS — Household (4)
        // ══════════════════════════════════════════════════════════════════════

        prod("Dish Soap",
                "Concentrated anti-grease lemon liquid dishwash. 1 litre bottle.",
                catHousehold, "Vim", "litre", 99, 85, 150);

        prod("Floor Cleaner",
                "Disinfectant floor cleaner that kills 99.9% of germs. 1 litre.",
                catHousehold, "Lizol", "litre", 149, 129, 100);

        prod("Laundry Detergent",
                "Front-load liquid detergent for tough stain removal. 1 kg.",
                catHousehold, "Ariel", "kg", 299, 259, 80);

        prod("Garbage Bags",
                "Heavy-duty 24 L garbage bags, extra-strong and leak-proof. 30 pack.",
                catHousehold, "Ezee", "30 pack", 79, 69, 200);

        // ══════════════════════════════════════════════════════════════════════
        //  PRODUCTS — Personal Care (3)
        // ══════════════════════════════════════════════════════════════════════

        Product shampoo = prod("Shampoo",
                "Advanced anti-dandruff formula with zinc pyrithione. 340 ml.",
                catPersonal, "Head And Shoulders", "340ml", 299, 249, 100);

        prod("Hand Wash",
                "Gentle moisturizing liquid soap with aloe vera. 250 ml pump.",
                catPersonal, "Dettol", "250ml", 89, 75, 200);

        prod("Toothpaste",
                "Whitening formula toothpaste with fluoride protection. 150g.",
                catPersonal, "Colgate", "150g", 99, 85, 180);

        // ══════════════════════════════════════════════════════════════════════
        //  PRODUCTS — Meat & Seafood (3)
        // ══════════════════════════════════════════════════════════════════════

        prod("Chicken Breast",
                "Skinless boneless chicken breast, fresh-cut. 500g pack.",
                catMeat, "FreshMeat", "500g", 199, 179, 60);

        prod("Salmon Fillet",
                "Atlantic salmon fillet, rich in Omega-3 acids. 300g pack.",
                catMeat, "OceanFresh", "300g", 449, 399, 40);

        prod("Prawns",
                "Medium shell-on prawns, cleaned and deveined. 250g pack.",
                catMeat, "SeaKing", "250g", 249, 219, 50);

        log.info("SwiftCart › 30 products created across 8 categories.");

        // ══════════════════════════════════════════════════════════════════════
        //  ADDRESS
        // ══════════════════════════════════════════════════════════════════════

        Address homeAddress = addressRepository.save(Objects.requireNonNull(Address.builder()
                .user(rahul)
                .street("42 MG Road, Koramangala")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560034")
                .label("Home")
                .isDefault(true)
                .build()));

        // ══════════════════════════════════════════════════════════════════════
        //  ORDER 1 — DELIVERED  (Bananas×2 + Milk×3 + Eggs×1)
        //  Total = 39×2 + 65×3 + 79×1 = 78 + 195 + 79 = 352
        // ══════════════════════════════════════════════════════════════════════

        BigDecimal o1Total = bd(39).multiply(bd(2))
                .add(bd(65).multiply(bd(3)))
                .add(bd(79).multiply(bd(1)));   // = 352.00

        Order order1 = orderRepository.save(Objects.requireNonNull(Order.builder()
                .user(rahul)
                .address(homeAddress)
                .totalAmount(o1Total)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(o1Total)
                .paymentMethod("CASH_ON_DELIVERY")
                .paymentStatus(Order.PaymentStatus.PAID)
                .orderStatus(Order.OrderStatus.DELIVERED)
                .deliveryInstructions("Please leave at the door if nobody home")
                .build()));

        saveItem(order1, bananas, 2, 39);
        saveItem(order1, milk,    3, 65);
        saveItem(order1, eggs,    1, 79);

        // ══════════════════════════════════════════════════════════════════════
        //  ORDER 2 — PROCESSING  (Chips×2 + OrangeJuice×1 + Shampoo×1)
        //  Total = 28×2 + 99×1 + 249×1 = 56 + 99 + 249 = 404
        // ══════════════════════════════════════════════════════════════════════

        BigDecimal o2Total = bd(28).multiply(bd(2))
                .add(bd(99).multiply(bd(1)))
                .add(bd(249).multiply(bd(1)));  // = 404.00

        Order order2 = orderRepository.save(Objects.requireNonNull(Order.builder()
                .user(rahul)
                .address(homeAddress)
                .totalAmount(o2Total)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(o2Total)
                .paymentMethod("CASH_ON_DELIVERY")
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.PROCESSING)
                .build()));

        saveItem(order2, chips,       2, 28);
        saveItem(order2, orangeJuice, 1, 99);
        saveItem(order2, shampoo,     1, 249);

        log.info("SwiftCart › 2 sample orders created (DELIVERED ₹352 + PROCESSING ₹404).");

        // ══════════════════════════════════════════════════════════════════════
        //  REVIEWS
        // ══════════════════════════════════════════════════════════════════════

        reviewRepository.save(Objects.requireNonNull(Review.builder()
                .product(milk).user(rahul).rating(5)
                .title("Great quality milk")
                .comment("Absolutely love this milk — fresh, creamy and delivered on time every day!")
                .isVerifiedPurchase(true).build()));

        reviewRepository.save(Objects.requireNonNull(Review.builder()
                .product(milk).user(rahul).rating(4)
                .title("Always fresh")
                .comment("Never had a bad batch. Consistently great quality and packaging.")
                .isVerifiedPurchase(true).build()));

        reviewRepository.save(Objects.requireNonNull(Review.builder()
                .product(milk).user(rahul).rating(5)
                .title("Value for money")
                .comment("Best quality-price ratio for full cream milk — highly recommended!")
                .isVerifiedPurchase(true).build()));

        reviewRepository.save(Objects.requireNonNull(Review.builder()
                .product(bananas).user(rahul).rating(4)
                .title("Fresh and ripe")
                .comment("Perfectly ripened bananas — great taste and ideal size. Will order again!")
                .isVerifiedPurchase(true).build()));

        reviewRepository.save(Objects.requireNonNull(Review.builder()
                .product(apples).user(rahul).rating(5)
                .title("Crunchy Shimla apples")
                .comment("So fresh and crispy! Much better quality than the local market.")
                .isVerifiedPurchase(true).build()));

        log.info("SwiftCart › 5 reviews created.");
        log.info("✅ SwiftCart initialization complete!");
        log.info("   Admin   : admin@swiftcart.com / admin123");
        log.info("   Customer: rahul@example.com / customer123");
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Category cat(String name, String description, String imgText) {
        return categoryRepository.save(Objects.requireNonNull(Category.builder()
                .name(name)
                .description(description)
                .imageUrl("https://placehold.co/120x120/E8F5E9/2E7D32?text=" + imgText)
                .build()));
    }

    private Product prod(String name, String description, Category category,
                         String brand, String unit, int price, int discPrice, int stock) {
        return productRepository.save(Objects.requireNonNull(Product.builder()
                .name(name)
                .description(description)
                .category(category)
                .brand(brand)
                .unit(unit)
                .price(BigDecimal.valueOf(price))
                .discountPrice(BigDecimal.valueOf(discPrice))
                .stockQuantity(stock)
                .isActive(true)
                .imageUrl("https://placehold.co/300x300/e8f5e9/2E7D32?text=" + name.replace(" ", "+"))
                .build()));
    }

    private void saveItem(Order order, Product product, int qty, int unitCost) {
        BigDecimal unitPrice = BigDecimal.valueOf(unitCost);
        BigDecimal subtotal  = unitPrice.multiply(BigDecimal.valueOf(qty));
        orderItemRepository.save(Objects.requireNonNull(OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(qty)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .build()));
    }

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
