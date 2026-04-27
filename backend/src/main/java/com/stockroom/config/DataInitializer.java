package com.stockroom.config;

import com.stockroom.model.Category;
import com.stockroom.model.Product;
import com.stockroom.model.User;
import com.stockroom.repository.CategoryRepository;
import com.stockroom.repository.ProductRepository;
import com.stockroom.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs once on startup. Seeds:
 *   - 2 users:        admin (ADMIN role) and viewer (VIEWER role)
 *   - 5 categories
 *   - 10 sample products
 *
 * Also creates the upload directory if it doesn't exist.
 *
 * K8s teaching point:
 *   SEED_ADMIN_PASSWORD is injected from a K8s Secret.
 *   Change the Secret value and do a rolling restart — the new password
 *   only takes effect if the user row is wiped first (or add a reset endpoint).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository     userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository  productRepository;
    private final PasswordEncoder    passwordEncoder;
    private final AppProperties      props;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder,
                           AppProperties props) {
        this.userRepository     = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository  = productRepository;
        this.passwordEncoder    = passwordEncoder;
        this.props              = props;
    }

    @Override
    public void run(String... args) throws Exception {
        createUploadDirectory();
        seedUsers();
        seedCategories();
        seedProducts();
    }

    private void createUploadDirectory() {
        try {
            Files.createDirectories(Path.of(props.getUpload().getPath()));
            log.info("Upload directory: {}", props.getUpload().getPath());
        } catch (Exception e) {
            log.warn("Could not create upload directory: {}", e.getMessage());
        }
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;
        log.info("Seeding users...");

        // Admin — full CRUD. Password from SEED_ADMIN_PASSWORD (K8s Secret)
        userRepository.save(new User("admin",
                passwordEncoder.encode(props.getSeed().getAdminPassword()), "ADMIN"));

        // Viewer — read-only (can browse but not modify)
        userRepository.save(new User("viewer",
                passwordEncoder.encode("viewer123"), "VIEWER"));

        log.info("Created users: admin / viewer");
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;
        log.info("Seeding categories...");

        categoryRepository.saveAll(List.of(
                new Category("Electronics",    "Electronic devices and accessories"),
                new Category("Clothing",       "Apparel and fashion items"),
                new Category("Food & Beverage","Consumable products"),
                new Category("Tools",          "Hand tools and power tools"),
                new Category("Books",          "Books and publications")
        ));
    }

    private void seedProducts() {
        if (productRepository.count() > 0) return;
        log.info("Seeding products...");

        Category elec = categoryRepository.findByName("Electronics").orElse(null);
        Category clth = categoryRepository.findByName("Clothing").orElse(null);
        Category tool = categoryRepository.findByName("Tools").orElse(null);
        Category book = categoryRepository.findByName("Books").orElse(null);

        productRepository.saveAll(List.of(
                p("MacBook Pro 14\"",      "Apple laptop with M3 Pro chip, 18GB RAM",  "MBP-001", "1299.99", 15, elec),
                p("iPhone 15 Pro",         "Latest iPhone with titanium design",        "IPH-001", "999.99",  42, elec),
                p("USB-C Hub 7-in-1",      "HDMI, USB-A×3, SD card, PD charging",     "USB-001", "49.99",  120, elec),
                p("Wireless Keyboard",     "Mechanical switches, 2.4GHz dongle",       "KBD-001", "129.99",  55, elec),
                p("27\" 4K Monitor",       "IPS panel, 144Hz, USB-C input",            "MON-001", "449.99",  18, elec),
                p("Developer Hoodie",      "100% cotton, front pocket, unisex",        "HOD-001", "69.99",   85, clth),
                p("Cargo Pants",           "Durable work pants with tool pockets",     "PNT-001", "59.99",   60, clth),
                p("Soldering Iron Kit",    "Professional 60W kit with accessories",    "SOL-001", "34.99",   30, tool),
                p("Precision Screwdriver Set","64-bit magnetic precision screwdrivers","SCR-001", "24.99",   75, tool),
                p("Clean Code (Book)",     "Robert C. Martin — A Handbook of Agile",   "BK-001",  "39.99",   40, book)
        ));

        log.info("Seeded {} products", productRepository.count());
    }

    private Product p(String name, String desc, String sku,
                      String price, int qty, Category cat) {
        return new Product(name, desc, sku, new BigDecimal(price), qty, cat);
    }
}
