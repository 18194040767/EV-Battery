# EV-BatterySecondLife

EV-BatterySecondLife is a full-stack platform for electric vehicle battery second-life management. It covers battery archive management, health assessment, marketplace trading, contract verification, logistics tracking, operations dashboards, messaging, and an admin AI assistant.

## Tech Stack

- Frontend: Vue 3, Vite, Element Plus, Pinia, Vue Router, ECharts
- Backend: Spring Boot 2.7, MyBatis-Plus, Spring Security, JWT, Redis, MySQL
- ML: Python model inference for battery SOH prediction
- Documents: PDF contract generation and verification support

## Repository Structure

```text
.
├── dataset/                         # Sample battery, BMS, and contract test data
├── ev-battery-platform-backend/      # Spring Boot backend API
│   ├── src/main/java/com/evbattery/
│   │   ├── modules/admin             # Admin dashboard and management APIs
│   │   ├── modules/assessment        # Battery assessment and ML prediction flow
│   │   ├── modules/battery           # Battery archive features
│   │   ├── modules/contract          # Contract generation and verification
│   │   ├── modules/logistics         # Logistics tracking
│   │   ├── modules/message           # Notification/message center
│   │   ├── modules/statistics        # Operational statistics
│   │   ├── modules/trade             # Product, cart, and order trading APIs
│   │   └── modules/user              # Authentication and user profile APIs
│   └── src/main/resources            # Spring configuration
├── ev-battery-platform-frontend/     # Vue frontend application
│   ├── public/                       # UI image assets
│   └── src/
│       ├── api                       # API clients
│       ├── components                # Shared UI components
│       ├── layout                    # User/admin layouts
│       ├── store                     # Pinia stores
│       └── views                     # Page views
├── ml/                               # Battery health model and inference scripts
├── tools/                            # Utility scripts
└── alter_tables.sql                  # Database migration helper SQL
```

Generated files such as `node_modules/`, `dist/`, `target/`, runtime logs, and local storage outputs are intentionally ignored and should not be committed.

## Prerequisites

- JDK 8+
- Maven 3.8+
- Node.js 18+
- Python 3.8+
- MySQL 8+
- Redis

## Backend Setup

1. Create the MySQL database and import the required schema/data for the project.
2. Update `ev-battery-platform-backend/src/main/resources/application-dev.yml` with your local database and Redis settings.
3. Optional: set `ZHIPU_API_KEY` if you want to enable the AI assistant backed by the configured LLM provider.
4. Start the backend:

```bash
cd ev-battery-platform-backend
mvn spring-boot:run
```

By default the backend ML configuration calls `../ml/predict.py`.

## Frontend Setup

```bash
cd ev-battery-platform-frontend
npm install
npm run dev
```

For production build verification:

```bash
npm run build
```

## ML Setup

```bash
cd ml
pip install -r requirements.txt
python predict.py
```

The repository includes sample model artifacts and data for local prediction and testing.

## Main Features

- User registration, login, profile, and admin account management
- Battery archive browsing, detail pages, and health assessment workflow
- SOH prediction through the Python ML module
- Marketplace product listing, product detail, cart, orders, and user trade flows
- Contract creation, PDF generation, and verification
- Logistics tracking and map-based delivery views
- Admin dashboard, user/product/order/battery/contract/statistics management
- Message center and admin AI assistant

## Notes

- Do not commit generated runtime files. The root `.gitignore` excludes common logs, caches, build outputs, backend storage, Maven targets, frontend dependencies, and Vite output.
- Large dependencies should be restored with package managers instead of stored in Git.
- Keep private keys, database passwords, and API keys out of source control. Use environment variables or local configuration files.
