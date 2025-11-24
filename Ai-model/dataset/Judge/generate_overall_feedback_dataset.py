import json
import random
import statistics
from pathlib import Path

# ===== Config =====
OUTPUT_FILE = Path("judge_overall_feedback_dataset_5k.jsonl")
TARGET_SESSIONS = 5000
RANDOM_SEED = 99
random.seed(RANDOM_SEED)

# ===== 1. KNOWLEDGE BASE (SUB-TOPIC AWARE) =====

ROLES = [
    "Backend", "Frontend", "DevOps", "Fullstack", "Mobile", "AI Engineer",
    "Data Engineer", "QA Engineer", "Security Engineer", "Cloud Architect",
    "System Administrator", "Site Reliability Engineer", "Platform Engineer",
    "Database Administrator", "ML Engineer", "iOS Developer", "Android Developer"
]

SKILLS = [
    # Frontend
    "React", "Vue.js", "Angular", "Next.js", "TypeScript", "JavaScript", "CSS", "HTML",
    "Svelte", "SolidJS", "Remix", "Nuxt.js", "Gatsby", "Tailwind CSS", "SASS", "Webpack",
    "Vite", "Redux", "MobX", "Zustand", "Recoil", "jQuery", "Bootstrap", "Material-UI",
    # Backend
    "Java", "Python", "Go", "Node.js", "Ruby", "PHP", "C#", ".NET", "Rust",
    "Scala", "Elixir", "Clojure", "Haskell", "Erlang", "Perl", "Lua", "R",
    "Spring Boot", "Django", "Flask", "FastAPI", "Express.js", "NestJS", "Rails",
    "Laravel", "ASP.NET Core", "Gin", "Echo", "Fiber", "Phoenix", "Play Framework",
    # Mobile
    "Swift", "Kotlin", "React Native", "Flutter", "Objective-C", "Java Android",
    "Xamarin", "Ionic", "Cordova", "SwiftUI", "Jetpack Compose", "UIKit",
    # Data & AI
    "TensorFlow", "PyTorch", "Pandas", "Scikit-learn", "Apache Spark", "Keras",
    "NumPy", "SciPy", "Matplotlib", "Seaborn", "XGBoost", "LightGBM", "CatBoost",
    "Hugging Face", "LangChain", "OpenAI API", "NLTK", "spaCy", "OpenCV",
    "JAX", "MLflow", "Weights & Biases", "Apache Flink", "Dask", "Ray",
    # DevOps & Cloud
    "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Terraform", "Jenkins", "GitLab CI",
    "GitHub Actions", "CircleCI", "Travis CI", "Ansible", "Chef", "Puppet", "SaltStack",
    "Prometheus", "Grafana", "ELK Stack", "Datadog", "New Relic", "Splunk",
    "Nginx", "Apache", "HAProxy", "Consul", "Vault", "Istio", "Linkerd",
    "ArgoCD", "Flux", "Helm", "Kustomize", "CloudFormation", "Pulumi",
    # Database
    "PostgreSQL", "MySQL", "MongoDB", "Redis", "Cassandra", "Elasticsearch",
    "Oracle", "SQL Server", "MariaDB", "SQLite", "DynamoDB", "CouchDB",
    "Neo4j", "ArangoDB", "InfluxDB", "TimescaleDB", "RocksDB", "Memcached",
    "Couchbase", "Aerospike", "ScyllaDB", "ClickHouse", "Snowflake", "BigQuery",
    # Message Queues & Streaming
    "Kafka", "RabbitMQ", "ActiveMQ", "NATS", "Pulsar", "Amazon SQS", "Amazon SNS",
    "Redis Streams", "ZeroMQ", "Apache Camel", "Google Pub/Sub",
    # API & Integration
    "GraphQL", "gRPC", "REST", "WebSockets", "Socket.IO", "Protobuf",
    "Apache Thrift", "Swagger", "OpenAPI", "Postman", "Insomnia",
    # Testing & Quality
    "Jest", "Mocha", "Chai", "Cypress", "Selenium", "Playwright", "Puppeteer",
    "JUnit", "TestNG", "pytest", "unittest", "RSpec", "PHPUnit", "Cucumber",
    "SonarQube", "ESLint", "Prettier", "Black", "Pylint", "Checkstyle",
    # Build Tools
    "Maven", "Gradle", "Ant", "npm", "yarn", "pnpm", "pip", "Poetry",
    "Cargo", "Bundler", "Composer", "Make", "Bazel", "Buck",
    # Version Control
    "Git", "GitHub", "GitLab", "Bitbucket", "Mercurial", "SVN", "Perforce",
    # Monitoring & Observability
    "Prometheus", "Grafana", "Jaeger", "Zipkin", "OpenTelemetry", "Sentry",
    # Security
    "OAuth", "JWT", "SAML", "SSL/TLS", "OWASP", "Snyk", "Trivy", "Vault",
    # Other
    "Microservices", "Serverless", "Lambda", "API Gateway", "Load Balancing",
    "Caching", "CDN", "DNS", "HTTP/HTTPS", "TCP/IP", "WebRTC"
]

# Từ vựng chuyên ngành phân theo Sub-topic (MASSIVE EXPANSION)
SKILL_DATA = {
    "React": {
        "performance": ["Virtual DOM", "Memoization", "re-renders", "lazy loading", "code splitting", "bundle optimization", "React.memo", "useMemo", "Tree shaking", "SSR", "ISR", "Suspense", "Concurrent features", "Profiler"],
        "state": ["Redux", "Context API", "Zustand", "Props", "State lifting", "Recoil", "MobX", "useReducer", "Redux Toolkit", "Jotai", "Valtio", "XState"],
        "hooks": ["useEffect", "useCallback", "custom hooks", "rules of hooks", "useRef", "useMemo", "useContext", "useLayoutEffect", "useImperativeHandle", "useTransition", "useDeferredValue", "useId"],
        "testing": ["Jest", "React Testing Library", "Enzyme", "unit tests", "integration tests", "Vitest", "Storybook", "Cypress", "MSW"],
        "routing": ["React Router", "navigation", "dynamic routes", "protected routes", "nested routes", "lazy routes", "TanStack Router"],
        "ecosystem": ["Next.js", "Remix", "Gatsby", "Create React App", "Vite", "Webpack", "Babel", "TypeScript"]
    },
    "Vue.js": {
        "reactivity": ["Reactive system", "Computed properties", "Watchers", "Proxies", "ref", "reactive", "readonly", "shallowRef", "triggerRef"],
        "composition": ["Composition API", "Options API", "Composables", "setup()", "script setup", "provide/inject"],
        "ecosystem": ["Vuex", "Pinia", "Vue Router", "Nuxt.js", "VueUse", "Vite", "Vue CLI", "Quasar"],
        "directives": ["v-if", "v-for", "v-model", "v-bind", "v-on", "custom directives", "v-slot"],
        "performance": ["Virtual DOM", "Compiler optimizations", "Tree shaking", "SSR", "SSG", "Lazy loading"]
    },
    "Angular": {
        "core": ["Components", "Modules", "Services", "Dependency Injection", "Decorators", "Templates", "Directives", "Pipes"],
        "routing": ["Router", "Guards", "Resolvers", "Lazy loading", "Preloading strategies"],
        "state": ["RxJS", "Observables", "Subjects", "NgRx", "Akita", "NGXS"],
        "forms": ["Template-driven", "Reactive forms", "Validators", "FormBuilder", "FormGroup"],
        "performance": ["Change detection", "OnPush", "TrackBy", "AOT compilation", "Tree shaking"]
    },
    "Java": {
        "memory": ["Garbage Collection", "Heap", "Stack", "Memory Leaks", "G1GC", "ZGC", "Shenandoah", "JVM tuning", "OutOfMemoryError", "PermGen", "Metaspace", "Escape analysis"],
        "concurrency": ["Multithreading", "Synchronized", "ExecutorService", "CompletableFuture", "Lock", "Semaphore", "ConcurrentHashMap", "Thread pools", "ForkJoinPool", "Phaser", "CountDownLatch", "CyclicBarrier", "Virtual threads"],
        "ecosystem": ["Spring Boot", "Maven", "Hibernate", "JPA", "Gradle", "Spring Cloud", "Microservices", "Spring Security", "Spring Data", "Spring WebFlux", "Quarkus", "Micronaut"],
        "streams": ["Stream API", "Lambda expressions", "Collectors", "Parallel streams", "Optional", "Method references"],
        "design": ["Design patterns", "SOLID principles", "Dependency Injection", "Singleton", "Factory", "Builder", "Observer", "Strategy"],
        "collections": ["ArrayList", "HashMap", "TreeMap", "LinkedList", "HashSet", "PriorityQueue", "Collections API"],
        "io": ["NIO", "Buffers", "Channels", "Selectors", "File I/O", "Serialization", "Streams"]
    },
    "Python": {
        "core": ["GIL", "Decorators", "Generators", "List Comprehensions", "Context managers", "Magic methods", "Metaclasses", "Iterators", "Descriptors", "Properties"],
        "async": ["Asyncio", "Coroutines", "Event Loop", "Await", "async/await", "concurrent.futures", "Threading", "Multiprocessing", "AsyncIO", "Trio"],
        "data": ["Pandas", "NumPy", "DataFrames", "Vectorization", "Series", "GroupBy", "Merge/Join", "Pivot tables", "Time series"],
        "web": ["Django", "Flask", "FastAPI", "SQLAlchemy", "Celery", "Tornado", "Sanic", "Starlette", "Pydantic"],
        "testing": ["pytest", "unittest", "mocking", "fixtures", "hypothesis", "tox", "coverage"],
        "ml": ["TensorFlow", "PyTorch", "Scikit-learn", "XGBoost", "Keras", "Hugging Face", "Transformers"],
        "tools": ["Poetry", "Pipenv", "virtualenv", "pip", "setuptools", "Black", "Pylint", "mypy", "ruff"]
    },
    "Go": {
        "concurrency": ["Goroutines", "Channels", "Select", "WaitGroups", "Mutexes", "Context", "sync.Pool"],
        "web": ["Gin", "Echo", "Fiber", "Chi", "net/http", "Gorilla Mux"],
        "patterns": ["Interfaces", "Composition", "Embedding", "Error handling", "Defer", "Panic/Recover"],
        "performance": ["Profiling", "Benchmarking", "Memory management", "Garbage Collection", "Escape analysis"],
        "tools": ["go mod", "go test", "go build", "go fmt", "golangci-lint", "delve"]
    },
    "Node.js": {
        "async": ["Event Loop", "Promises", "async/await", "Callbacks", "Streams", "EventEmitter", "Worker threads"],
        "performance": ["Clustering", "Worker threads", "Memory profiling", "V8 optimization", "PM2", "Load balancing"],
        "ecosystem": ["Express", "NestJS", "TypeScript", "npm", "package.json", "Fastify", "Koa", "Hapi"],
        "streams": ["Readable", "Writable", "Transform", "Duplex", "Piping", "Backpressure"],
        "security": ["Helmet", "CORS", "Rate limiting", "Input validation", "JWT", "OAuth"]
    },
    "TypeScript": {
        "types": ["Type inference", "Generics", "Union types", "Intersection types", "Type guards", "Conditional types", "Mapped types", "Template literal types"],
        "advanced": ["Utility types", "Decorators", "Mixins", "Namespaces", "Module augmentation"],
        "config": ["tsconfig.json", "Strict mode", "Compiler options", "Type declarations", "DefinitelyTyped"],
        "patterns": ["Type-safe state management", "Branded types", "Discriminated unions", "Builder pattern"]
    },
    "Docker": {
        "build": ["Dockerfile", "Multi-stage builds", "Image layers", "Cache", "BuildKit", ".dockerignore", "Build arguments", "Labels"],
        "run": ["Containers", "Volumes", "Networking", "Compose", "Port mapping", "Environment variables", "Health checks", "Resource limits"],
        "orch": ["Kubernetes", "Swarm", "Services", "Replicas", "Pods", "Deployments", "Helm", "StatefulSets", "DaemonSets"],
        "security": ["Image scanning", "Least privilege", "Secret management", "Network policies", "User namespaces", "Rootless containers"],
        "networking": ["Bridge", "Host", "Overlay", "Macvlan", "Service discovery", "Load balancing"],
        "storage": ["Bind mounts", "Volumes", "tmpfs", "Volume drivers", "Backup strategies"]
    },
    "Kubernetes": {
        "core": ["Pods", "Deployments", "Services", "ConfigMaps", "Secrets", "ReplicaSets", "Jobs", "CronJobs"],
        "scaling": ["HPA", "VPA", "Cluster Autoscaler", "Resource limits", "Resource requests", "QoS classes"],
        "networking": ["Ingress", "NetworkPolicy", "Service mesh", "CNI", "CoreDNS", "kube-proxy", "Calico", "Cilium"],
        "storage": ["PersistentVolumes", "StatefulSets", "StorageClass", "CSI drivers", "Volume snapshots"],
        "security": ["RBAC", "Pod Security", "Network Policies", "Secrets encryption", "Admission controllers"],
        "monitoring": ["Prometheus", "Grafana", "Metrics Server", "Logging", "Tracing", "Alerts"]
    },
    "AWS": {
        "compute": ["EC2", "Lambda", "ECS", "Fargate", "Auto Scaling", "Batch", "Lightsail", "App Runner"],
        "storage": ["S3", "EBS", "EFS", "Glacier", "FSx", "Storage Gateway", "Snowball"],
        "database": ["RDS", "DynamoDB", "Aurora", "ElastiCache", "DocumentDB", "Neptune", "Timestream"],
        "networking": ["VPC", "Route 53", "CloudFront", "Load Balancers", "API Gateway", "Direct Connect", "Transit Gateway"],
        "security": ["IAM", "KMS", "Secrets Manager", "WAF", "Shield", "GuardDuty", "Security Hub"],
        "monitoring": ["CloudWatch", "X-Ray", "CloudTrail", "Config", "Systems Manager"],
        "messaging": ["SQS", "SNS", "EventBridge", "Kinesis", "MQ", "MSK"]
    },
    "Azure": {
        "compute": ["VMs", "App Service", "Functions", "AKS", "Container Instances", "Batch"],
        "storage": ["Blob Storage", "Files", "Disks", "Archive", "Data Lake"],
        "database": ["SQL Database", "Cosmos DB", "PostgreSQL", "MySQL", "Cache for Redis"],
        "networking": ["VNet", "Load Balancer", "Application Gateway", "Front Door", "CDN"],
        "security": ["Active Directory", "Key Vault", "Security Center", "Sentinel"]
    },
    "PostgreSQL": {
        "performance": ["Indexes", "Query optimization", "EXPLAIN", "Vacuum", "Partitioning", "Statistics", "Parallel queries", "Work_mem tuning"],
        "transactions": ["ACID", "Isolation levels", "Locks", "Deadlocks", "MVCC", "Two-phase commit"],
        "advanced": ["CTEs", "Window functions", "JSON operations", "Full-text search", "Arrays", "Stored procedures", "Triggers"],
        "replication": ["Streaming replication", "Logical replication", "Hot standby", "Failover", "WAL"],
        "extensions": ["PostGIS", "pg_stat_statements", "pgcrypto", "uuid-ossp", "TimescaleDB"]
    },
    "MongoDB": {
        "core": ["Documents", "Collections", "BSON", "ObjectId", "Embedded documents", "References"],
        "queries": ["Find", "Aggregation", "Indexes", "Text search", "Geospatial queries"],
        "performance": ["Indexes", "Sharding", "Replication", "Connection pooling", "Query optimization"],
        "advanced": ["Change streams", "Transactions", "Atlas", "GridFS", "Time series"],
        "modeling": ["Embedding vs referencing", "Schema design", "Denormalization", "Polymorphic patterns"]
    },
    "Redis": {
        "data": ["Strings", "Lists", "Sets", "Sorted sets", "Hashes", "Streams", "Bitmaps", "HyperLogLog"],
        "patterns": ["Caching", "Session storage", "Pub/Sub", "Rate limiting", "Leaderboards", "Job queues"],
        "advanced": ["Lua scripts", "Transactions", "Pipelining", "Redis Cluster", "Sentinel", "Persistence"],
        "performance": ["Memory optimization", "Eviction policies", "TTL", "Connection pooling"]
    },
    "Kafka": {
        "core": ["Topics", "Partitions", "Producers", "Consumers", "Brokers", "ZooKeeper", "KRaft"],
        "patterns": ["Event sourcing", "CQRS", "Stream processing", "CDC", "Log aggregation"],
        "advanced": ["Consumer groups", "Offset management", "Exactly-once semantics", "Transactions", "Idempotence"],
        "ecosystem": ["Kafka Streams", "Kafka Connect", "Schema Registry", "ksqlDB", "Confluent"],
        "performance": ["Partitioning strategy", "Compression", "Batching", "Replication factor"]
    },
    "GraphQL": {
        "core": ["Schema", "Queries", "Mutations", "Subscriptions", "Resolvers", "Types", "Interfaces"],
        "advanced": ["DataLoader", "N+1 problem", "Fragments", "Directives", "Custom scalars", "Federation"],
        "tools": ["Apollo", "Relay", "Hasura", "Prisma", "GraphQL Code Generator"],
        "patterns": ["Schema design", "Error handling", "Authentication", "Caching", "Batching"]
    },
    "Spring Boot": {
        "core": ["Auto-configuration", "Starter dependencies", "Embedded servers", "Application properties", "Profiles"],
        "web": ["REST Controllers", "Request mapping", "Exception handling", "Validation", "CORS"],
        "data": ["JPA", "Repositories", "Transactions", "Query methods", "Specifications", "Auditing"],
        "security": ["Authentication", "Authorization", "JWT", "OAuth2", "CSRF", "Method security"],
        "advanced": ["Actuator", "Caching", "Async processing", "WebFlux", "Batch processing", "Integration testing"]
    },
    "Django": {
        "core": ["Models", "Views", "Templates", "URLs", "Middleware", "Settings", "Apps"],
        "orm": ["QuerySets", "Migrations", "Relationships", "Aggregation", "F expressions", "Q objects"],
        "advanced": ["Class-based views", "Generic views", "Forms", "Model forms", "Admin customization"],
        "rest": ["Django REST Framework", "Serializers", "ViewSets", "Authentication", "Permissions", "Throttling"],
        "async": ["ASGI", "Async views", "Channels", "WebSockets"]
    },
    "FastAPI": {
        "core": ["Path operations", "Pydantic models", "Dependency injection", "Type hints", "Async support"],
        "advanced": ["Background tasks", "WebSockets", "GraphQL", "OpenAPI", "OAuth2", "JWT"],
        "patterns": ["Request validation", "Response models", "Error handling", "Middleware", "CORS"],
        "performance": ["Async/await", "Starlette", "Uvicorn", "Connection pooling"]
    },
    "TensorFlow": {
        "core": ["Tensors", "Graphs", "Sessions", "Keras API", "Eager execution", "tf.function", "AutoGraph"],
        "training": ["Optimizers", "Loss functions", "Backpropagation", "Gradient descent", "Learning rate schedules", "Callbacks"],
        "deployment": ["TensorFlow Serving", "TFLite", "SavedModel", "ONNX", "TensorFlow.js", "TFX"],
        "advanced": ["Custom layers", "Custom training loops", "Mixed precision", "Distributed training", "TPU support"],
        "data": ["tf.data", "Data augmentation", "Pipelines", "Prefetching", "Caching"]
    },
    "PyTorch": {
        "core": ["Tensors", "Autograd", "nn.Module", "DataLoader", "Optimizers", "Loss functions"],
        "training": ["Training loops", "Gradient descent", "Backpropagation", "Learning rate schedulers", "Early stopping"],
        "deployment": ["TorchScript", "ONNX", "TorchServe", "Mobile deployment", "Quantization"],
        "advanced": ["Custom layers", "Hooks", "Distributed training", "Mixed precision", "Lightning"],
        "ecosystem": ["Hugging Face", "PyTorch Geometric", "Torchvision", "Torchaudio"]
    },
    "Terraform": {
        "core": ["Resources", "Providers", "State", "Variables", "Outputs", "Modules", "Data sources"],
        "advanced": ["Remote state", "Workspaces", "Provisioners", "Dynamic blocks", "For_each", "Count"],
        "patterns": ["Module composition", "State management", "Secrets handling", "Multi-environment"],
        "tools": ["Terraform Cloud", "Atlantis", "Terragrunt", "Checkov", "tfsec"]
    },
    "Jenkins": {
        "core": ["Pipelines", "Jobs", "Agents", "Plugins", "Credentials", "Webhooks"],
        "pipeline": ["Declarative", "Scripted", "Jenkinsfile", "Stages", "Steps", "Parallel execution"],
        "advanced": ["Shared libraries", "Blue Ocean", "Docker integration", "Kubernetes plugin"],
        "patterns": ["CI/CD", "GitOps", "Multi-branch", "Matrix builds", "Artifacts"]
    },
    # Fallback chung cho các skill khác
    "Generic": {
        "arch": ["MVC", "Microservices", "Monolith", "Event-driven", "CQRS", "Hexagonal", "Serverless", "SOA", "Clean Architecture", "Domain-Driven Design"],
        "quality": ["Testing", "CI/CD", "Code Review", "Documentation", "Refactoring", "Tech debt", "Static analysis", "Code coverage", "Performance testing"],
        "patterns": ["Factory", "Observer", "Strategy", "Adapter", "Singleton", "Repository", "Decorator", "Proxy", "Command", "Chain of Responsibility"],
        "principles": ["SOLID", "DRY", "KISS", "YAGNI", "Separation of concerns", "Law of Demeter", "Composition over inheritance"],
        "security": ["Authentication", "Authorization", "Encryption", "Input validation", "SQL injection", "XSS", "CSRF", "OWASP Top 10"],
        "performance": ["Caching", "Load balancing", "Horizontal scaling", "Vertical scaling", "Database indexing", "Query optimization"],
        "distributed": ["CAP theorem", "Eventual consistency", "Strong consistency", "Distributed transactions", "Saga pattern", "Circuit breaker"]
    }
}

# --- Mảnh ghép Câu trả lời (Lego Parts) ---

OPENERS = [
    "That's a good question.",
    "Well, basically,",
    "From what I know,",
    "In my experience,",
    "To put it simply,",
    "Technically speaking,",
    "This is a core concept where",
    "Honestly, I think",
    "If I remember correctly,",
    "Usually,",
    "From a practical standpoint,",
    "In production environments,",
    "Based on my understanding,",
    "The way I see it,",
    "According to best practices,",
    "Let me explain:",
    "Interestingly,",
    "Actually,",
    "To be clear,",
    "In this context,",
    "Fundamentally,",
    "The key thing is,",
    "What I've learned is,",
    "From a technical perspective,",
    "Generally speaking,",
    "In my previous projects,",
    "The standard approach is,",
    "It's worth noting that,",
    "Let me think for a second,",
    "If we look at it realistically,"
]

CORE_TEMPLATES = {
    "performance": [
        "{skill} optimizes performance by using {kw1}.",
        "To avoid bottlenecks, {skill} often relies on {kw1}.",
        "A lot of the performance in {skill} comes from proper use of {kw1}.",
        "{kw1} is crucial for {skill} performance optimization.",
        "By implementing {kw1}, {skill} usually achieves better throughput.",
        "The key to scaling {skill} is understanding {kw1}.",
        "{skill} leverages {kw1} to minimize overhead.",
        "Performance gains in {skill} often come from optimizing {kw1}."
    ],
    "memory": [
        "{skill} manages memory mainly through {kw1}.",
        "You need to be careful with {kw1} to prevent leaks.",
        "Proper {kw1} usage is essential for memory efficiency in {skill}.",
        "{kw1} helps {skill} handle memory allocation more predictably.",
        "Understanding {kw1} prevents common memory issues in {skill}.",
        "{skill} often relies on {kw1} for efficient memory management."
    ],
    "async": [
        "{skill} handles asynchronous operations using {kw1}.",
        "{kw1} enables non-blocking I/O in {skill}.",
        "Concurrency in {skill} is usually achieved through {kw1}.",
        "{skill} processes async tasks via {kw1}.",
        "With {kw1}, {skill} can scale to many concurrent operations."
    ],
    "state": [
        "{skill} manages state primarily through {kw1}.",
        "State synchronization in {skill} often relies on {kw1}.",
        "{kw1} provides predictable state management for {skill}.",
        "Complex state in {skill} is typically handled by {kw1}.",
        "{kw1} helps avoid inconsistent state in larger {skill} codebases."
    ],
    "testing": [
        "{skill} ensures quality through {kw1}.",
        "Testing in {skill} usually utilizes the {kw1} framework.",
        "{kw1} helps validate {skill} application behavior end-to-end.",
        "With {kw1}, it's easier to prevent regressions in {skill} projects."
    ],
    "security": [
        "{skill} implements security via {kw1}.",
        "Protecting against vulnerabilities often requires understanding {kw1}.",
        "{kw1} is essential for securing {skill} applications.",
        "In hardened environments, {skill} usually combines {kw1} with other controls."
    ],
    "generic": [
        "The main mechanism involves {kw1} and {kw2}.",
        "It allows developers to handle {kw1} more efficiently.",
        "{skill} is designed around the concept of {kw1}.",
        "{kw1} works together with {kw2} to enable {skill} functionality.",
        "The core principle of {skill} is based on {kw1}.",
        "{skill} implements {kw1} to solve common problems.",
        "Developers use {kw1} in {skill} for better maintainability.",
        "{kw1} is a fundamental building block of {skill}.",
        "A lot of real-world usages of {skill} revolve around {kw1}.",
        "In practice, {kw1} ends up being the critical piece in {skill}."
    ]
}

DETAILS = {
    "performance": [
        "This significantly reduces latency in production.",
        "It helps in handling high throughput scenarios.",
        "Ideally, this keeps the application responsive under load.",
        "We measured clear performance improvement after implementing this.",
        "This approach scales well when traffic spikes.",
        "Caching at this level dramatically improves response times.",
        "Profiling usually shows this eliminates the main bottleneck.",
        "In practice, this reduces CPU and memory consumption noticeably."
    ],
    "memory": [
        "This prevents out-of-memory errors in long-lived processes.",
        "Garbage collection handles a lot of this automatically in most cases.",
        "Proper cleanup here is critical for background services.",
        "Memory profiling often reveals this as a leak source.",
        "Monitoring shows more stable memory usage with this approach.",
        "This reduces heap pressure significantly over time."
    ],
    "async": [
        "This enables handling thousands of concurrent connections.",
        "Non-blocking I/O here improves overall system throughput.",
        "The event loop efficiently manages multiple operations.",
        "This prevents thread blocking and improves responsiveness.",
        "It also reduces the need for large thread pools."
    ],
    "state": [
        "This ensures predictable state transitions.",
        "A single source of truth simplifies debugging considerably.",
        "State updates are tracked and auditable this way.",
        "This pattern prevents race conditions in state updates.",
        "It also makes it easier to reason about complex flows."
    ],
    "testing": [
        "Automated tests catch regressions early in the pipeline.",
        "This improves code coverage and confidence before release.",
        "Mock objects help isolate units under test.",
        "Integration tests validate end-to-end workflows.",
        "A solid test suite here speeds up refactoring later."
    ],
    "security": [
        "This mitigates common attack vectors like injection.",
        "Encryption at rest and in transit is essential.",
        "Input validation prevents malicious payloads from reaching core logic.",
        "Security audits often highlight this as a critical control.",
        "Combined with monitoring, this reduces the blast radius of incidents."
    ],
    "generic": [
        "This helps in reducing technical debt significantly.",
        "Basically, it decouples the logic from the implementation.",
        "I've used this pattern in previous projects with good results.",
        "Code reviews consistently recommend this approach.",
        "This aligns well with industry best practices.",
        "The team adopted this after seeing clear benefits.",
        "Documentation usually emphasizes this as the preferred method.",
        "Refactoring to this pattern improved maintainability.",
        "This design choice paid off once the system had to scale.",
        "Other teams in the company use this same pattern successfully."
    ]
}

CLOSERS = [
    "So yeah, that's the gist of it.",
    "That's how I would approach it.",
    "It's a fundamental part of the ecosystem.",
    "Hope that makes sense.",
    "That more or less covers the basics.",
    "In production, we handle this pretty carefully.",
    "This is a common pattern in the industry.",
    "That's the standard approach most teams take.",
    "Does that answer your question?",
    "Let me know if you want more details.",
    "I think that explains it fairly well.",
    "That's been my experience with it so far.",
    "This is widely adopted in modern development.",
    "It's pretty straightforward once you break it down.",
    "That's what I've learned from practice.",
    "Most developers I know follow this pattern.",
    "It's also well-documented in the official guides.",
    "This approach has served us quite well.",
    "That's how we implement it in our codebase.",
    "You’ll see this everywhere in production systems.",
    "It's basically the textbook solution for this.",
    "Real-world use cases confirm this works.",
    "Best practices generally suggest this direction."
]

# ===== Feedback templates cho per-question feedback =====

FEEDBACK_EXCELLENT = [
    "**Outstanding depth**: Comprehensive understanding with specific technical details.",
    "**Strong accuracy**: All concepts explained correctly with proper terminology.",
    "**Excellent structure**: Well-organized answer following logical flow.",
    "**Production-ready thinking**: Considered real-world implications and best practices.",
    "**Clear communication**: Complex topics explained in accessible manner.",
    "**Expert-level insight**: Demonstrated advanced knowledge beyond basic requirements.",
    "**Concrete examples**: Provided specific use cases and practical scenarios.",
    "**Trade-off awareness**: Discussed pros/cons and alternative approaches.",
    "**Industry alignment**: Answer reflects current best practices and standards.",
    "**Problem-solving focus**: Showed how concept solves real technical challenges.",
    "**Architectural understanding**: Connected concept to broader system design.",
    "**Performance considerations**: Addressed efficiency and optimization aspects.",
    "**Security awareness**: Mentioned relevant security implications.",
    "**Scalability mindset**: Discussed how solution handles growth.",
    "**Edge case handling**: Considered unusual scenarios and error conditions."
]

FEEDBACK_GOOD = [
    "**Good understanding**: Covers main concepts adequately with reasonable depth.",
    "**Solid foundation**: Demonstrates core knowledge effectively.",
    "**Clear communication**: Explains ideas in organized manner.",
    "**Practical awareness**: Shows understanding of real-world applications.",
    "**Correct fundamentals**: Core technical concepts explained accurately.",
    "**Logical flow**: Answer progresses naturally from point to point.",
    "**Relevant examples**: Included appropriate use cases when helpful.",
    "**Confidence**: Demonstrated comfort level with the topic.",
    "**Standard practices**: Aligned with common industry approaches.",
    "**Adequate coverage**: Addressed key aspects of the question.",
    "**Technical vocabulary**: Used proper terminology appropriately.",
    "**Conceptual clarity**: Made abstract ideas understandable."
]

FEEDBACK_AVERAGE = [
    "**Basic grasp**: Shows awareness of concepts but lacks depth.",
    "**Missing details**: Answer remains surface-level without specifics.",
    "**Needs structure**: Could benefit from better organization.",
    "**Limited examples**: Would benefit from concrete use cases.",
    "**Incomplete coverage**: Missed some important aspects of the question.",
    "**Vague terminology**: Used imprecise language in places.",
    "**Shallow explanation**: Didn't explore the 'why' behind concepts.",
    "**Minimal elaboration**: Stuck to basic definitions without expansion.",
    "**Lack of context**: Didn't connect to real-world scenarios.",
    "**Uncertain delivery**: Hesitation suggests incomplete understanding.",
    "**Generic response**: Answer could apply to many technologies.",
    "**Missing nuance**: Oversimplified complex topics."
]

FEEDBACK_POOR = [
    "**Fundamental gaps**: Limited understanding of core concepts.",
    "**Lacks substance**: Missing critical technical details.",
    "**Unclear communication**: Explanation difficult to follow.",
    "**Critical need**: Requires foundational learning before proceeding.",
    "**Incorrect information**: Contains factual errors or misconceptions.",
    "**No structure**: Disorganized thoughts without clear progression.",
    "**Minimal content**: Answer too brief to demonstrate knowledge.",
    "**Confused concepts**: Mixed up different technologies or patterns.",
    "**No practical understanding**: Unable to explain real-world application.",
    "**Terminology misuse**: Incorrect use of technical terms.",
    "**Tangential response**: Answer didn't address the actual question.",
    "**Surface memorization**: Recited buzzwords without understanding.",
    "**Unable to elaborate**: Couldn't expand when prompted.",
    "**Contradictory statements**: Answer contains logical inconsistencies."
]

# ===== 2. HELPER CHO SUBTOPIC & OVERALL FEEDBACK =====

GENERIC_SUBTOPIC_LABELS = {
    "arch": "architecture",
    "quality": "code quality and testing",
    "patterns": "design patterns",
    "principles": "design principles",
    "security": "security best practices",
    "performance": "performance and scalability",
    "distributed": "distributed systems"
}

QUESTION_TEMPLATES = [
    # Classic direct questions
    "How does {skill} handle {kw}?",
    "What is the role of {kw} in {skill}?",
    "Can you explain {kw} in the context of {skill}?",
    "What are the best practices for {subtopic_label} in {skill}?",

    # Explain / Describe
    "Explain how {kw} works inside {skill}.",
    "Describe how you would use {kw} when working with {skill}.",
    "Explain the relationship between {kw} and {subtopic_label} in {skill}.",

    # Why / Reasoning
    "Why is {kw} important when building systems with {skill}?",
    "Why does {subtopic_label} matter when using {skill} in production?",

    # Scenario-based
    "Imagine you are building a production system with {skill}. How would you approach {kw}?",
    "Suppose you have a problem related to {kw} in {skill}. What would you do?",
    "If a bug appears around {kw} in {skill}, how would you debug it?",

    # Best practices / Pitfalls
    "What are common pitfalls related to {kw} when using {skill}?",
    "What best practices do you follow for {subtopic_label} in {skill}?",

    # Design / Architecture
    "Walk me through how you would design {subtopic_label} using {skill}.",
    "How would you integrate {kw} into a larger architecture built on {skill}?",

    # Comparison
    "How does {kw} in {skill} compare to alternative approaches or tools?",
    "When would you avoid using {kw} in {skill}, and why?",

    # Experience-based
    "In your experience, what are the key challenges around {kw} in {skill}?",
    "Can you share a situation where {kw} in {skill} made a big difference to your system?",

    # Trade-offs / decision making
    "How do you think about trade-offs when using {kw} in {skill}?",
    "If you had to redesign a feature around {kw} in {skill}, what would you change?",
]

FOLLOWUP_TEMPLATES = [
    # Clarification / Dig deeper
    "Earlier you mentioned **{kw}** in {skill}. Could you expand on what you meant by that?",
    "You brought up **{kw}**. Can you go deeper into how it actually works under the hood?",
    "You briefly mentioned **{kw}** — can you explain its role in {skill} in more detail?",
    "Can you clarify how **{kw}** interacts with other components when using {skill}?",
    "You said **{kw}** is important. Why exactly is it critical in real systems?",

    # Challenge knowledge / trade-offs
    "What are the trade-offs of using **{kw}** in a production environment with {skill}?",
    "Is there any situation where you would avoid using **{kw}** in {skill}? Why?",
    "How do you decide between using **{kw}** and another alternative in {skill}?",
    "You seem confident about **{kw}** — what would you consider its biggest limitation?",
    "If **{kw}** becomes a bottleneck, how would you approach solving that?",

    # Debugging / failure thinking
    "Imagine **{kw}** suddenly behaves incorrectly in production. How would you debug that?",
    "If a critical incident happens related to **{kw}**, what steps would you take to investigate?",
    "What metrics or logs would you look at to troubleshoot issues around **{kw}**?",

    # Scenario-based deepening
    "Suppose your system needs to scale quickly. How does **{kw}** affect your design choices?",
    "If you had to refactor code that heavily uses **{kw}**, how would you approach it?",
    "How would **{kw}** behave under extreme load, and what would you watch out for?",

    # Compare / contrast follow-ups
    "You mentioned **{kw}**. How does it compare to other patterns or tools solving the same problem?",
    "What would be a reasonable alternative to **{kw}**, and when would it be better?",
    "What mistakes do engineers commonly make when using **{kw}** in {skill}?",

    # Push deeper into reasoning
    "Why do you think **{kw}** is the right approach here instead of other options?",
    "What underlying principle in {skill} makes **{kw}** a good fit?",
    "If you were reviewing someone else's code using **{kw}**, what would you look for?",

    # Push on experience
    "Have you faced real issues with **{kw}** before? What happened and what did you learn?",
    "Can you share a real-world scenario where **{kw}** significantly improved or degraded system behavior?",
    "Which part of **{kw}** do you still find tricky or easy to get wrong?"
]


def pretty_subtopic(skill, subtopic):
    """Hiển thị subtopic đẹp hơn trong câu hỏi / assessment."""
    if skill not in SKILL_DATA or skill == "Generic":
        return GENERIC_SUBTOPIC_LABELS.get(subtopic, subtopic)
    return subtopic.replace("_", " ")


def get_skill_keywords(skill, subtopic=None):
    data = SKILL_DATA.get(skill, SKILL_DATA["Generic"])
    if subtopic and subtopic in data:
        return data[subtopic]
    all_kws = []
    for _, v in data.items():
        all_kws.extend(v)
    return all_kws or ["core concepts"]


def add_noise(text):
    """Thêm nhiễu tự nhiên (Natural Noise) cho câu trả lời"""
    # 20% giữ sạch
    if random.random() > 0.8:
        return text

    # 1. Lowercase toàn câu thỉnh thoảng
    if random.random() > 0.5:
        text = text.lower()

    # 2. Thêm filler words
    fillers = ["um,", "uh,", "like,", "you know,"]
    words = text.split()
    if random.random() > 0.6 and len(words) > 3:
        insert_pos = random.randint(1, len(words) - 2)
        words.insert(insert_pos, random.choice(fillers))
        text = " ".join(words)

    # 3. Bỏ dấu chấm cuối
    if text.endswith(".") and random.random() > 0.5:
        text = text[:-1]

    return text


def generate_answer(skill, quality="high", subtopic="generic"):
    """Lắp ghép câu dựa trên Sub-topic"""
    keywords = get_skill_keywords(skill, subtopic)
    kw1 = random.choice(keywords)
    kw2 = random.choice(keywords)

    opener = random.choice(OPENERS)
    closer = random.choice(CLOSERS)

    templates = CORE_TEMPLATES.get(subtopic, CORE_TEMPLATES["generic"])
    core_tmpl = random.choice(templates)

    detail_list = DETAILS.get(subtopic, DETAILS["generic"])
    detail = random.choice(detail_list)

    if quality == "high":
        core = core_tmpl.format(skill=skill, kw1=kw1, kw2=kw2)
        raw_text = f"{opener} {core} {detail} {closer}"
    elif quality == "avg":
        core = core_tmpl.format(skill=skill, kw1=kw1, kw2=kw2)
        raw_text = f"{opener} {core} {detail}"
    else:
        raw_text = f"Um, I think {skill} uses {kw1}? I'm not really sure regarding {subtopic}."

    return add_noise(raw_text)


def generate_question(skill, subtopic="generic"):
    keywords = get_skill_keywords(skill, subtopic)
    kw = random.choice(keywords)
    subtopic_label = pretty_subtopic(skill, subtopic)

    template = random.choice(QUESTION_TEMPLATES)
    return template.format(
        skill=skill,
        kw=kw,
        subtopic_label=subtopic_label
    )

# ===== 3. OVERALL FEEDBACK POOLS (ASSESSMENT / STRENGTHS / WEAKNESSES / RECOMMENDATIONS) =====

ASSESSMENT_TEMPLATES = {
    "EXCELLENT": [
        "The interview demonstrated the candidate’s **exceptional mastery** of {skill}, especially regarding **{subtopic_label}**.",
        "They provided highly accurate and nuanced responses, scoring **{pct}%** across {num_questions} questions.",
        "Their explanations showed not only depth but also clarity, connecting abstract ideas to real-world system behavior.",
        "They consistently demonstrated awareness of edge cases, scalability concerns, and operational implications.",
        "The candidate presented solutions with a strong architectural mindset, reflecting experience beyond their seniority level.",
        "Their ability to break down complex technical concepts indicates strong communication and mentoring capability.",
        "They showcased familiarity with industry best practices and modern tooling around {skill}.",
        "Their reasoning process was strong: they compared alternatives, evaluated trade-offs, and justified decisions.",
        "They exhibited production-grade intuition, including performance bottlenecks, failure handling, and maintainability.",
        "Their responses reflected hands-on experience with challenging scenarios related to {subtopic_label}.",
        "They handled follow-up questions smoothly and confidently, adjusting explanations based on context.",
        "Overall, the candidate is **highly suitable** for a {seniority} {role} position and may even exceed expectations at this level.",
        "Their thought process resembles that of a well-rounded engineer familiar with real-world constraints.",
        "They demonstrated fast reasoning, strong synthesis skills, and the ability to generalize concepts across domains.",
        "They show strong potential for technical leadership roles in the future."
    ],

    "GOOD": [
        "The candidate showed **solid competency** in {skill}, especially around **{subtopic_label}**, achieving a score of **{pct}%**.",
        "They demonstrated a reliable grasp of key concepts and provided mostly accurate answers to {num_questions} questions.",
        "Their explanations were generally clear, though occasionally lacked deeper detail or examples.",
        "They were able to describe practical applications of {skill} and how it fits into typical system workflows.",
        "Their reasoning was correct, though sometimes linear and missing broader trade-off discussions.",
        "They handled follow-up questions reasonably well, even if briefly.",
        "The candidate meets expectations for a {seniority} {role} developer and shows good potential to grow further.",
        "They demonstrated comfort discussing common patterns and real-world situations related to {skill}.",
        "Their ability to articulate technical decisions was good but can improve with more hands-on production exposure.",
        "Communication was structured, albeit occasionally too high-level.",
        "With focused effort on advanced concepts, they could reach an excellent performance band.",
        "They showed consistency across questions, indicating steady foundational strength."
    ],

    "AVERAGE": [
        "The candidate demonstrated **partial understanding** of {skill}, particularly related to **{subtopic_label}**.",
        "They delivered mixed performance across {num_questions} questions, resulting in an overall score of **{pct}%**.",
        "They recognized key terms but struggled to provide precise or complete explanations.",
        "Their answers tended to remain conceptual rather than practical or implementation-focused.",
        "They occasionally gave correct ideas but without connecting them to real-world engineering scenarios.",
        "Communication was acceptable but sometimes disorganized, affecting clarity.",
        "They showed potential but need substantial improvement in applying concepts in depth.",
        "They lacked confidence when asked to justify reasoning or compare alternatives.",
        "Their understanding appears based more on theory or surface learning than practical experience.",
        "Several areas of {subtopic_label} were addressed only partially or inaccurately.",
        "Overall, the candidate meets baseline requirements but is not yet fully ready for advanced tasks."
    ],

    "BELOW AVERAGE": [
        "The candidate showed **limited understanding** of {skill} and struggled with questions about **{subtopic_label}**.",
        "Their performance across {num_questions} questions resulted in a below-expectation score of **{pct}%**.",
        "Answers commonly lacked detail, accuracy, or relevance to the question.",
        "They had difficulty explaining foundational concepts or relating them to practical usage.",
        "Communication lacked structure, making reasoning hard to follow.",
        "They appeared unfamiliar with common tools, patterns, and workflows associated with {skill}.",
        "Many explanations were vague and showed signs of guesswork rather than understanding.",
        "They struggled to answer follow-up questions or go beyond surface definitions.",
        "Their familiarity with {subtopic_label} appears minimal, with frequent confusion about terminology.",
        "At this time, they **do not meet** expectations for a {seniority} {role} developer.",
        "Significant upskilling is needed before they can handle role-related responsibilities."
    ],

    "POOR": [
        "The candidate demonstrated **major gaps in fundamental knowledge** of {skill}, especially regarding **{subtopic_label}**.",
        "Their overall score of **{pct}%** indicates insufficient readiness for a {seniority} {role} role.",
        "They were unable to explain essential concepts or articulate how technologies fit together.",
        "Most responses lacked clarity, correctness, and connection to real-world engineering.",
        "They showed confusion and frequently contradicted themselves while discussing basic topics.",
        "Their difficulty answering follow-up questions suggests a lack of foundational understanding.",
        "They demonstrated little awareness of practical usage or industry best practices.",
        "Communication issues further obscured their technical thinking.",
        "A complete restart from the fundamentals is recommended before attempting this interview again.",
        "They would benefit significantly from guided learning and structured hands-on practice."
    ]
}

STRENGTH_POOLS = {
    "EXCELLENT": [
        "Shows exceptional clarity in articulating complex engineering concepts.",
        "Demonstrates strong architectural awareness across multiple layers of the system.",
        "Provides thoughtful comparisons between approaches, emphasizing trade-offs.",
        "Presents ideas with confidence and strong logical flow.",
        "Consistently brings up reliability, resilience, and maintainability considerations.",
        "Shows deep familiarity with production failures, bottlenecks, and real scenarios.",
        "Communicates like someone experienced in mentoring or code reviews.",
        "Balances precision with readability when explaining technical details.",
        "Understands both high-level architecture and low-level implementation."
    ],
    "GOOD": [
        "Demonstrates reliable understanding of **{skill}** fundamentals.",
        "Provides coherent explanations with appropriate structure.",
        "Shows awareness of common pitfalls and best practices in real projects.",
        "Gives practical examples when prompted.",
        "Shows confidence in familiar areas of the stack.",
        "Understands how {subtopic_label} applies in everyday engineering work.",
        "Reasoning is generally sound and technically aligned."
    ],
    "AVERAGE": [
        "Shows willingness to explain reasoning and think through problems.",
        "Demonstrates partial familiarity with technical terminology.",
        "Attempts to apply {subtopic_label} concepts in context.",
        "Able to discuss simple examples with some clarity.",
        "Maintains professionalism and stays engaged throughout.",
        "Recognizes core ideas even when missing deeper insight."
    ],
    "BELOW AVERAGE": [
        "Shows interest in improving and is open to learning.",
        "Attempts to understand questions even when unsure.",
        "Displays positive attitude and willingness to engage.",
        "Shows basic recognition of high-level concepts."
    ],
    "POOR": [
        "Maintains a cooperative attitude despite difficulty.",
        "Shows interest in entering the engineering field.",
        "Recognizes very high-level keywords related to the topic.",
        "Attempts to answer even when unsure, indicating motivation."
    ]
}

WEAKNESS_POOLS = {
    "EXCELLENT": [
        "Could expand further on cross-team communication patterns or leadership narratives.",
        "May benefit from more explicit reasoning when discussing trade-offs at scale.",
        "Sometimes compresses insights too much assuming high audience familiarity.",
        "Could offer more concrete examples for extremely advanced edge-case scenarios."
    ],
    "GOOD": [
        "Still developing deeper intuition for advanced {subtopic_label} patterns.",
        "Occasionally misses subtle details related to scalability or failure modes.",
        "Examples sometimes rely on personal intuition rather than data-driven reasoning.",
        "Lacks some hands-on exposure with large-scale or complex distributed systems.",
        "Could compare alternative solutions in {skill} more thoroughly."
    ],
    "AVERAGE": [
        "Fundamental concepts in {skill} need reinforcement before tackling advanced tasks.",
        "Explanations lack specificity and real-world grounding.",
        "Reasoning becomes unclear under follow-up or probing questions.",
        "Examples provided were too generic or unrelated to practical usage.",
        "Struggles to explain interactions between components in a system."
    ],
    "BELOW AVERAGE": [
        "Gaps in core {skill} fundamentals severely impact explanation quality.",
        "Frequently mixes unrelated concepts, indicating lack of grounding.",
        "Unable to connect terminology to practical engineering workflows.",
        "Struggles to recall or apply essential ideas in {subtopic_label}.",
        "Has difficulty maintaining logical flow in responses."
    ],
    "POOR": [
        "Lacks foundational understanding of engineering basics.",
        "Demonstrates significant confusion around common concepts.",
        "Unable to describe simple use cases or scenarios.",
        "Shows no evidence of hands-on experience with {skill}.",
        "Reasoning lacks structure and consistency."
    ]
}

RECOMMENDATION_POOLS = {
    "EXCELLENT": [
        "Consider taking on system design ownership to further grow architectural influence.",
        "Contribute more actively to code reviews or team-wide technical decisions.",
        "Explore leadership tracks or mentor junior engineers to strengthen management skills.",
        "Stay engaged with community standards, RFCs, and new ecosystem proposals.",
        "Deep dive into advanced {subtopic_label} case studies to sharpen edge-case intuition.",
        "Present your knowledge via internal tech talks or external conferences."
    ],

    "GOOD": [
        "Build deeper experience with production workloads to strengthen decision-making.",
        "Develop stronger intuition for trade-offs in {skill} by implementing multiple real-world solutions.",
        "Practice breaking down complex topics concisely for clearer technical communication.",
        "Engage in cross-functional work to strengthen architectural breadth.",
        "Enhance practical expertise with targeted hands-on projects involving {subtopic_label}.",
        "Focus on scaling patterns, reliability techniques, and observability best practices."
    ],

    "AVERAGE": [
        "Reinforce fundamentals through structured courses or documentation study.",
        "Practice implementing real projects that use {skill} end-to-end.",
        "Use guided coding challenges to strengthen reasoning and hands-on capability.",
        "Study open-source repositories to learn idiomatic code and common patterns.",
        "Focus on mastering one sub-area (such as {subtopic_label}) before expanding further.",
        "Prepare a study roadmap to improve consistency over the next few months."
    ],

    "BELOW AVERAGE": [
        "Start with foundational courses that rebuild understanding of {skill} and software basics.",
        "Engage with a mentor to ensure guided learning and feedback loops.",
        "Work through progressively complex mini-projects to gain confidence.",
        "Avoid advanced topics initially; focus on fundamentals and terminology.",
        "Spend consistent practice sessions (6–12 months) reinforcing the basics.",
        "Follow structured tutorials to gain predictable progress."
    ],

    "POOR": [
        "Begin with introductory computer science fundamentals before specializing.",
        "Join a beginner-friendly bootcamp or structured learning environment.",
        "Focus on very small, achievable projects to build learning momentum.",
        "Avoid interviews temporarily and prioritize skill building for 12–18 months.",
        "Seek mentorship or peer study groups to guide early practice.",
        "Develop core software literacy before approaching complex tools like {skill}."
    ]
}


def sample_items(pool, min_k, max_k):
    if not pool:
        return []
    max_k_eff = min(max_k, len(pool))
    min_k_eff = min(min_k, max_k_eff)
    if max_k_eff <= 0:
        return []
    k = random.randint(min_k_eff, max_k_eff)
    return random.sample(pool, k=k)


def build_assessment(overview, skill, main_subtopic, seniority, role, num_questions, avg_score):
    pct = int(avg_score * 100)
    subtopic_label = pretty_subtopic(skill, main_subtopic)
    templates = ASSESSMENT_TEMPLATES[overview]
    sentences = sample_items(templates, 4, 6)
    ctx = {
        "skill": skill,
        "subtopic_label": subtopic_label,
        "seniority": seniority,
        "role": role,
        "num_questions": num_questions,
        "pct": pct,
    }
    return " ".join(s.format(**ctx) for s in sentences)


def build_strengths(overview, skill, main_subtopic, seniority, role):
    subtopic_label = pretty_subtopic(skill, main_subtopic)
    pool = STRENGTH_POOLS[overview]
    items = sample_items(pool, 3, 5)
    ctx = {
        "skill": skill,
        "subtopic_label": subtopic_label,
        "seniority": seniority,
        "role": role,
    }
    return [s.format(**ctx) for s in items]


def build_weaknesses(overview, skill, main_subtopic, seniority, role):
    subtopic_label = pretty_subtopic(skill, main_subtopic)
    pool = WEAKNESS_POOLS[overview]
    items = sample_items(pool, 2, 4)
    ctx = {
        "skill": skill,
        "subtopic_label": subtopic_label,
        "seniority": seniority,
        "role": role,
    }
    return [s.format(**ctx) for s in items]


def build_recommendations(overview, skill, main_subtopic, seniority, role):
    subtopic_label = pretty_subtopic(skill, main_subtopic)
    pool = RECOMMENDATION_POOLS[overview]
    sentences = sample_items(pool, 3, 5)
    ctx = {
        "skill": skill,
        "subtopic_label": subtopic_label,
        "seniority": seniority,
        "role": role,
    }
    return " ".join(s.format(**ctx) for s in sentences)

# ===== 4. SESSION BUILDER =====

def create_session():
    role = random.choice(ROLES)
    seniority = random.choice(["Intern", "Junior", "Senior", "Lead"])
    skill = random.choice(SKILLS)

    # Xác định Sub-topic chính cho session
    skill_config = SKILL_DATA.get(skill, SKILL_DATA["Generic"])
    skill_subtopics = list(skill_config.keys())
    main_subtopic = random.choice(skill_subtopics)
    current_subtopic = main_subtopic
    last_subtopic_label = pretty_subtopic(skill, main_subtopic)

    num_questions = random.choices([3, 4], weights=[0.7, 0.3])[0]
    candidate_tier = random.choices(
        ["Excellent", "Good", "Average", "Weak"],
        weights=[0.15, 0.35, 0.35, 0.15]
    )[0]

    conversation = []
    all_scores = []

    for i in range(1, num_questions + 1):
        # Logic Follow-up: Câu 2 có thể hỏi sâu hơn
        is_follow_up = (i == 2 and random.random() > 0.5)

        if is_follow_up:
            kw = random.choice(get_skill_keywords(skill, current_subtopic))
            subtopic_label = pretty_subtopic(skill, current_subtopic)
            template = random.choice(FOLLOWUP_TEMPLATES)
            question = template.format(skill=skill, kw=kw, subtopic_label=subtopic_label)
        else:
            if random.random() > 0.7:
                current_subtopic = random.choice(skill_subtopics)
            question = generate_question(skill, current_subtopic)
            last_subtopic_label = pretty_subtopic(skill, current_subtopic)

        # Chất lượng câu trả lời
        if candidate_tier == "Excellent":
            q_qual = "high"
        elif candidate_tier == "Weak":
            q_qual = "low"
        else:
            q_qual = random.choices(["high", "avg", "low"], weights=[0.3, 0.5, 0.2])[0]

        answer = generate_answer(skill, q_qual, current_subtopic)

        # Scoring Logic
        if q_qual == "high":
            base = 0.88
        elif q_qual == "avg":
            base = 0.65
        else:
            base = 0.35

        final_score = min(1.0, max(0.1, base + random.uniform(-0.1, 0.1)))

        if final_score > 0.8:
            feedback = random.sample(FEEDBACK_EXCELLENT, k=min(3, len(FEEDBACK_EXCELLENT)))
        elif final_score >= 0.65:
            feedback = random.sample(FEEDBACK_GOOD, k=min(3, len(FEEDBACK_GOOD)))
        elif final_score >= 0.45:
            feedback = random.sample(FEEDBACK_AVERAGE, k=min(2, len(FEEDBACK_AVERAGE)))
        else:
            feedback = random.sample(FEEDBACK_POOR, k=min(2, len(FEEDBACK_POOR)))

        scores = {
            "correctness": round(min(1.0, final_score + random.uniform(-0.05, 0.05)), 2),
            "depth": round(min(1.0, final_score + random.uniform(-0.1, 0.1)), 2),
            "clarity": round(min(1.0, final_score + random.uniform(-0.05, 0.05)), 2),
            "practicality": round(min(1.0, final_score + random.uniform(-0.1, 0.1)), 2),
            "coverage": round(min(1.0, final_score + random.uniform(-0.1, 0.1)), 2),
            "final": round(final_score, 2)
        }
        all_scores.append(final_score)

        conversation.append({
            "sequence_number": i,
            "question": question,
            "answer": answer,
            "scores": scores,
            "feedback": feedback
        })

    # Final Assessment Construction
    avg_score = statistics.mean(all_scores)
    if avg_score >= 0.8:
        overview = "EXCELLENT"
    elif avg_score >= 0.65:
        overview = "GOOD"
    elif avg_score >= 0.45:
        overview = "AVERAGE"
    elif avg_score >= 0.30:
        overview = "BELOW AVERAGE"
    else:
        overview = "POOR"

    assessment = build_assessment(overview, skill, main_subtopic, seniority, role, num_questions, avg_score)
    strengths = build_strengths(overview, skill, main_subtopic, seniority, role)
    weaknesses = build_weaknesses(overview, skill, main_subtopic, seniority, role)
    recommendations = build_recommendations(overview, skill, main_subtopic, seniority, role)

    return {
        "input": {
            "conversation": conversation,
            "role": role,
            "seniority": seniority,
            "skills": [skill],
            "total_questions": num_questions
        },
        "output": {
            "overview": overview,
            "assessment": assessment,
            "strengths": strengths,
            "weaknesses": weaknesses,
            "recommendations": recommendations
        }
    }

# ===== Main Execution =====
if __name__ == "__main__":
    print("=" * 80)
    print("GENERATING COMPREHENSIVE JUDGE OVERALL FEEDBACK DATASET")
    print("=" * 80)
    print(f"\n🔨 Target: {TARGET_SESSIONS} sessions")
    print(f"📁 Output: {OUTPUT_FILE}\n")

    sessions = []
    for i in range(1, TARGET_SESSIONS + 1):
        s = create_session()
        sessions.append(s)

        if i % 1000 == 0:
            print(f"  Generated {i:,} sessions...")

    print(f"\n✅ Generated {len(sessions):,} sessions")

    overview_dist = {}
    for s in sessions:
        ov = s["output"]["overview"]
        overview_dist[ov] = overview_dist.get(ov, 0) + 1

    print("\n📊 Overview Distribution:")
    for ov in ["EXCELLENT", "GOOD", "AVERAGE", "BELOW AVERAGE", "POOR"]:
        count = overview_dist.get(ov, 0)
        pct = (count / len(sessions)) * 100
        print(f"   {ov}: {count:,} ({pct:.1f}%)")

    print(f"\n📝 Writing to: {OUTPUT_FILE}")
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        for s in sessions:
            f.write(json.dumps(s, ensure_ascii=False) + '\n')

    print(f"✅ Dataset created: {len(sessions):,} sessions")

    sample = sessions[0]
    print("\n📄 Sample session:")
    print(f"   Role: {sample['input']['role']}")
    print(f"   Seniority: {sample['input']['seniority']}")
    print(f"   Skills: {', '.join(sample['input']['skills'])}")
    print(f"   Questions: {sample['input']['total_questions']}")
    print(f"   Overview: {sample['output']['overview']}")
    print(f"   Strengths: {len(sample['output']['strengths'])} items")
    print(f"   Weaknesses: {len(sample['output']['weaknesses'])} items")

    print("\n" + "=" * 80)
    print("✅ DATASET GENERATION COMPLETE")
    print("=" * 80)
    print(f"\n📁 Output: {OUTPUT_FILE}")
    print(f"📊 Total: {len(sessions):,} sessions")
