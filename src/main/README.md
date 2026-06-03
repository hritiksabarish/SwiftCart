# 🛒 Swift Cart

Swift Cart is a full-stack grocery shopping web application that enables users to browse products, filter by categories, manage their shopping cart, and place orders through a seamless and responsive interface.

Built using **Spring Boot**, **MySQL**, and **Vanilla JavaScript**, the project demonstrates the integration of a RESTful backend with a dynamic frontend.

---

## 🚀 Features

* Browse grocery products
* View products by category
* Product search and filtering
* Add products to cart
* Update cart quantities
* Remove products from cart
* Responsive user interface
* REST API integration
* Database-driven product management

---

## 🛠️ Tech Stack

### Frontend

* HTML5
* CSS3
* JavaScript (ES6)

### Backend

* Spring Boot
* Spring MVC
* Spring Data JPA

### Database

* MySQL

### Tools

* Git & GitHub
* Postman
* VS Code
* IntelliJ IDEA

---

## 📂 Project Structure

```text
Swift-Cart/
│
├── frontend/
│   ├── css/
│   ├── js/
│   ├── images/
│   └── *.html
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── application.properties
│
└── README.md
```

## ⚙️ Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/swift-cart.git
cd swift-cart
```

### 2. Configure Database

Create a MySQL database:

```sql
CREATE DATABASE swiftcart;
```

Update database credentials in:

```properties
application.properties
```

### 3. Run Backend

```bash
cd grocery-store
mvn spring-boot:run
```

Backend will start on:

```text
http://localhost:8080
```

### 4. Run Frontend

Open the frontend folder using VS Code and launch with Live Server.

or

```bash
npx serve .
```

Frontend will run on:

```text
http://127.0.0.1:5500 or 8080 (depending on the default port)
```

---

## 🔌 API Endpoints

### Products

| Method | Endpoint                    | Description              |
| ------ | --------------------------- | ------------------------ |
| GET    | /api/products               | Get all products         |
| GET    | /api/products/{id}          | Get product by ID        |
| GET    | /api/products/category/{id} | Get products by category |

### Cart

| Method | Endpoint              | Description           |
| ------ | --------------------- | --------------------- |
| POST   | /api/cart/add         | Add item to cart      |
| PUT    | /api/cart/update      | Update quantity       |
| DELETE | /api/cart/remove/{id} | Remove item from cart |

---

## 🗄️ Database Design

### Product

* product_id
* product_name
* category
* price
* description
* stock_quantity
* image_url

### Cart

* cart_id
* product_id
* quantity

---




## 🎯 Learning Outcomes

Through this project, I gained hands-on experience with:

* REST API development using Spring Boot
* Frontend-backend integration
* MySQL database design
* JavaScript asynchronous programming
* Git and GitHub workflow
* Full-stack application architecture

---

## 🔮 Future Enhancements

* User Authentication & Authorization
* JWT-based Security
* Order Tracking
* Payment Gateway Integration
* Wishlist Functionality
* Admin Dashboard
* Product Reviews & Ratings

---

## 👨‍💻 Author

Hritik Sabarish

If you found this project interesting, feel free to star the repository.
