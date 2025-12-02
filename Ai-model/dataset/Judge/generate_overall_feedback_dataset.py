import json
import random
import statistics
from pathlib import Path

# ===== Config =====
OUTPUT_FILE = Path("judge_overall_feedback_dataset_100k.jsonl")
TARGET_SESSIONS = 50
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

# Từ vựng chuyên ngành phân theo Sub-topic
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
    # Fallback chung
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

# --- Answer Lego Parts ---

OPENERS = [
    # Standard/Professional
    "That's a good question.",
    "Let me explain:",
    "To be clear,",
    "According to best practices,",
    "From a technical perspective,",
    "Technically speaking,",
    "From an architectural perspective,",
    "The standard approach is,",
    "Generally speaking,",
    "The conventional wisdom suggests,",
    "The industry standard involves,",
    "According to industry standards,",
    
    # Experience-based
    "In my experience,",
    "From what I know,",
    "Drawing from experience,",
    "In my previous projects,",
    "Based on my understanding,",
    "What I've learned is,",
    "From what I've seen,",
    "In the projects I've worked on,",
    "From my time working with this,",
    "Based on what I've done before,",
    "Having worked with this extensively,",
    "After using this in production,",
    "From hands-on experience,",
    
    # Analytical
    "Well, basically,",
    "To put it simply,",
    "If we break this down,",
    "Fundamentally,",
    "The key thing is,",
    "A critical aspect here is,",
    "One key consideration is,",
    "The main point is,",
    "At its core,",
    "If we analyze this,",
    "Breaking it down,",
    "Looking at the fundamentals,",
    "When you think about it,",
    
    # Contextual
    "This is a core concept where",
    "In this context,",
    "In production environments,",
    "In real-world scenarios,",
    "From a practical standpoint,",
    "If we look at it realistically,",
    "In practical terms,",
    "When applied to production,",
    "In enterprise settings,",
    "For production systems,",
    "In real applications,",
    "In the real world,",
    
    # Thoughtful
    "The way I see it,",
    "My understanding is that,",
    "Honestly, I think",
    "If I remember correctly,",
    "Let me think for a second,",
    "Interesting question -",
    "That's an important topic.",
    "Good question.",
    "I'd say that",
    "The way I think about this is",
    "From my perspective,",
    "In my view,",
    
    # Engaging
    "Actually,",
    "Interestingly,",
    "What makes this interesting is,",
    "It's worth noting that,",
    "Here's the thing:",
    "What's interesting here is",
    "The cool part about this is",
    "One thing to know is",
    "Something worth mentioning:",
    "An important point is",
    
    # Casual/Natural
    "Usually,",
    "Typically,",
    "Most of the time,",
    "In general,",
    "Normally,",
    "As a rule,",
    "More often than not,",
    "Nine times out of ten,",
    "In most cases,",
    "From what I understand,",
    
    # Comparative
    "Compared to alternatives,",
    "Looking at different options,",
    "When you compare approaches,",
    "Relative to other solutions,",
    "If we compare this to",
    
    # Problem-solving focus
    "The challenge here is",
    "The problem this solves is",
    "What this addresses is",
    "This helps with",
    "The purpose of this is",
    "This was designed to",
    
    # Authority/Confidence
    "The established pattern is",
    "The proven approach involves",
    "Best practices dictate that",
    "The recommended way is",
    "The right way to think about this:",
    "The correct approach here is",
    
    # Discovery/Learning
    "What I've discovered is",
    "What becomes clear is",
    "What you find is that",
    "What I've noticed is",
    "The pattern I've seen is",
    
    # Direct
    "Simply put,",
    "In essence,",
    "At the end of the day,",
    "Bottom line is",
    "The short answer is",
    "To answer directly:",
    "Straight up,",
    
    # Empty/Minimal
    "",
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
        "Performance gains in {skill} often come from optimizing {kw1}.",
        "{skill} combines {kw1} with {kw2} for maximum efficiency.",
        "When tuning {skill}, focusing on {kw1} yields the biggest wins.",
        "The bottleneck analysis shows {kw1} is where {skill} spends most cycles.",
        "{skill}'s architecture specifically enables efficient {kw1}.",
        "Production systems running {skill} depend heavily on proper {kw1} configuration.",
        "Expert {skill} developers always optimize {kw1} first.",
        "{kw1} in {skill} can be fine-tuned using {kw2} techniques."
    ],
    "memory": [
        "{skill} manages memory mainly through {kw1}.",
        "You need to be careful with {kw1} to prevent leaks.",
        "Proper {kw1} usage is essential for memory efficiency in {skill}.",
        "{kw1} helps {skill} handle memory allocation more predictably.",
        "Understanding {kw1} prevents common memory issues in {skill}.",
        "{skill} often relies on {kw1} for efficient memory management.",
        "Memory pressure in {skill} is controlled by adjusting {kw1}.",
        "{skill} allocates and deallocates memory using {kw1} patterns.",
        "The {kw1} mechanism in {skill} prevents most memory-related crashes.",
        "Profiling {skill} applications usually reveals {kw1} as the key factor.",
        "{skill}'s memory model is built around {kw1} principles."
    ],
    "async": [
        "{skill} handles asynchronous operations using {kw1}.",
        "{kw1} enables non-blocking I/O in {skill}.",
        "Concurrency in {skill} is usually achieved through {kw1}.",
        "{skill} processes async tasks via {kw1}.",
        "With {kw1}, {skill} can scale to many concurrent operations.",
        "The {kw1} pattern in {skill} allows thousands of parallel requests.",
        "{skill}'s async model is based on {kw1} and {kw2}.",
        "Non-blocking execution in {skill} relies entirely on {kw1}.",
        "{skill} developers use {kw1} to avoid callback hell.",
        "The {kw1} abstraction in {skill} simplifies concurrent programming.",
        "{skill} combines {kw1} with traditional threading when needed.",
        "Async/await with {kw1} in {skill} eliminates callback pyramids.",
        "The event loop in {skill} uses {kw1} for non-blocking I/O.",
        "{skill}'s {kw1} handles 50k+ concurrent connections efficiently.",
        "Using {kw1}, our {skill} service maintains sub-100ms latency.",
    ],
    "state": [
        "{skill} manages state primarily through {kw1}.",
        "State synchronization in {skill} often relies on {kw1}.",
        "{kw1} provides predictable state management for {skill}.",
        "Complex state in {skill} is typically handled by {kw1}.",
        "{kw1} helps avoid inconsistent state in larger {skill} codebases.",
        "The state architecture in {skill} centers around {kw1}.",
        "{skill} uses {kw1} to maintain consistency across components.",
        "State updates in {skill} are handled atomically using {kw1}.",
        "{kw1} in {skill} ensures all components see the same data.",
        "Global state in {skill} applications is managed through {kw1}.",
        "{skill}'s {kw1} prevents race conditions in state mutations."
    ],
    "testing": [
        "{skill} ensures quality through {kw1}.",
        "Testing in {skill} usually utilizes the {kw1} framework.",
        "{kw1} helps validate {skill} application behavior end-to-end.",
        "With {kw1}, it's easier to prevent regressions in {skill} projects.",
        "The {skill} ecosystem provides {kw1} for comprehensive testing.",
        "{skill} developers rely on {kw1} for continuous integration.",
        "Test coverage in {skill} projects improves dramatically with {kw1}.",
        "{skill} combines {kw1} with {kw2} for different test levels.",
        "Quality assurance in {skill} starts with implementing {kw1}.",
        "{kw1} integrates seamlessly with {skill}'s development workflow."
    ],
    "security": [
        "{skill} implements security via {kw1}.",
        "Protecting against vulnerabilities often requires understanding {kw1}.",
        "{kw1} is essential for securing {skill} applications.",
        "In hardened environments, {skill} usually combines {kw1} with other controls.",
        "{skill} prevents common attacks by properly configuring {kw1}.",
        "Security best practices for {skill} always include {kw1}.",
        "{kw1} in {skill} provides defense against injection attacks.",
        "{skill}'s security model is built on {kw1} principles.",
        "Audits of {skill} applications focus heavily on {kw1} implementation.",
        "{skill} combines {kw1} with {kw2} for defense in depth.",
        "{skill} uses {kw1} to prevent XSS and CSRF attacks.",
        "JWT tokens with {kw1} provide stateless authentication in {skill}.",
        "HTTPS/TLS with {kw1} secures {skill} communications.",
        "OAuth2 and {kw1} handle authorization in {skill} APIs.",
        "RBAC with {kw1} controls access in {skill} applications.",
    ],
    "generic": [
        "The main mechanism involves {kw1} and {kw2}.",
        "The core principle of {skill} is based on {kw1}.",
        "{kw1} is a fundamental building block of {skill}.",
        "The underlying mechanism relies on {kw1}.",
        "{skill} is built on top of {kw1}.",
        "At its core, {skill} uses {kw1}.",
        "The foundation of {skill} is {kw1}.",
        "{kw1} forms the backbone of {skill}.",
        "{skill} is designed around the concept of {kw1}.",
        "{skill}'s architecture centers on {kw1}.",
        "The design philosophy of {skill} embraces {kw1}.",
        "{skill} was architected with {kw1} in mind.",
        "The architectural pattern involves {kw1}.",
        "{skill} implements {kw1} to solve common problems.",
        "{skill} provides built-in support for {kw1}.",
        "The {skill} framework abstracts {kw1} complexity elegantly.",
        "{skill} encapsulates {kw1} functionality.",
        "Under the hood, {skill} uses {kw1}.",
        "{skill} wraps {kw1} with a clean API.",
        "It allows developers to handle {kw1} more efficiently.",
        "{kw1} works together with {kw2} to enable {skill} functionality.",
        "{skill} combines {kw1} with {kw2} to achieve desired outcomes.",
        "{skill} leverages {kw1} to provide functionality.",
        "The power of {skill} comes from how it handles {kw1}.",
        "{skill} enables developers to work with {kw1} easily.",
        "{kw1} gives {skill} its unique capabilities.",
        "Developers use {kw1} in {skill} for better maintainability.",
        "A lot of real-world usages of {skill} revolve around {kw1}.",
        "In practice, {kw1} ends up being the critical piece in {skill}.",
        "{skill} developers commonly leverage {kw1} for common use cases.",
        "Teams typically rely on {kw1} when working with {skill}.",
        "Most {skill} projects make heavy use of {kw1}.",
        "You'll find {kw1} in almost every {skill} application.",
        "{kw1} integration in {skill} is straightforward and well-documented.",
        "{skill} provides seamless {kw1} integration.",
        "Working with {kw1} in {skill} is well-supported.",
        "The ecosystem around {skill} supports {kw1} extensively.",
        "{kw1} has first-class support in {skill}.",
        "{skill}'s approach to {kw1} differs from traditional methods.",
        "{skill} takes a unique approach to {kw1}.",
        "Unlike alternatives, {skill} handles {kw1} differently.",
        "{skill}'s take on {kw1} is more elegant.",
        "Compared to other tools, {skill}'s {kw1} is superior.",
        "Modern {skill} applications almost always utilize {kw1}.",
        "{skill} popularized the use of {kw1} in production systems.",
        "{kw1} has become standard in {skill} development.",
        "The {skill} community has embraced {kw1}.",
        "{kw1} is now ubiquitous in {skill} projects.",
        "Best practices in {skill} emphasize proper {kw1} usage.",
        "Following best practices means using {kw1} in {skill}.",
        "The recommended approach involves {kw1}.",
        "Experts suggest leveraging {kw1} when using {skill}.",
        "{kw1} improves code quality in {skill} projects.",
        "Using {kw1} makes {skill} applications more robust.",
        "{kw1} simplifies complex tasks in {skill}.",
        "Teams benefit from {kw1} when building with {skill}.",
        "Productivity increases when using {kw1} with {skill}.",
        "The {kw1} pattern is central to {skill}.",
        "{skill} embraces the {kw1} paradigm.",
        "Understanding {kw1} is key to mastering {skill}.",
        "{kw1} represents a key concept in {skill}.",
        "The {kw1} abstraction makes {skill} powerful.",
        "In production, {skill} relies heavily on {kw1}.",
        "Real-world {skill} apps depend on {kw1}.",
        "At scale, {kw1} becomes essential in {skill}.",
        "Enterprise {skill} deployments use {kw1} extensively.",
        "REST APIs in {skill} utilize {kw1} for clean interfaces.",
        "CRUD operations with {kw1} simplify {skill} development.",
        "GraphQL and {kw1} provide flexible queries in {skill}.",
        "gRPC with {kw1} offers high-performance RPC in {skill}.",
        "ORM layers use {kw1} to abstract database logic in {skill}.",
        "Microservices in {skill} communicate via {kw1}.",
        "The MVC pattern with {kw1} structures {skill} applications.",
        "DDD principles and {kw1} improve {skill} architecture.",
        "CI/CD pipelines deploy {skill} apps using {kw1}.",
        "K8s and {kw1} orchestrate {skill} containers.",
        "Learning {kw1} is crucial for {skill} development.",
        "Mastering {skill} means understanding {kw1}.",
        "You can't really know {skill} without knowing {kw1}.",
        "{kw1} is one of the first things you learn in {skill}.",
        "{skill} has evolved to embrace {kw1}.",
        "Modern {skill} is built around {kw1}.",
        "The latest {skill} versions focus on {kw1}.",
        "{skill} continues to improve {kw1} support.",
        "{kw1} solves key challenges in {skill}.",
        "Without {kw1}, {skill} would struggle with certain tasks.",
        "{kw1} addresses pain points in {skill} development.",
        "The {kw1} feature eliminates common issues.",
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
        "In practice, this reduces CPU and memory consumption noticeably.",
        "Benchmarks consistently show 2-3x improvement with this technique.",
        "Load testing revealed this handles 10k+ concurrent requests smoothly.",
        "Response times dropped from 500ms to under 50ms after optimization.",
        "This prevents the system from degrading under stress.",
        "We've seen P99 latency improve by an order of magnitude.",
        "Resource utilization stays below 60% even during peak hours.",
        "The optimization allows horizontal scaling without code changes.",
        "This approach reduces DB round trips significantly.",
        "Monitoring dashboards show sustained improvement across all metrics.",
        "QPS increased from 1k to 15k after this change.",
        "Our API latency dropped from 800ms to 80ms.",
        "TTL of 5 minutes reduced cache misses by 70%.",
        "RPS handling improved 5x with connection pooling.",
        "TTFB went from 1.2s to 150ms.",
        "We now handle 50k RPS on a single instance.",
        "Memory footprint decreased from 4GB to 1.5GB.",
        "CDN caching reduced origin requests by 85%.",
        "Database connection pool of 20 handles peak load.",
        "Redis caching gives us sub-10ms response times.",
        "Throughput increased from 500 TPS to 5k TPS.",
        "CPU usage dropped from 80% to 35% average.",
    ],
    "memory": [
        "This prevents OOM errors in long-lived processes.",
        "GC handles a lot of this automatically in most cases.",
        "Proper cleanup here is critical for background services.",
        "Memory profiling often reveals this as a leak source.",
        "Monitoring shows more stable memory usage with this approach.",
        "This reduces heap pressure significantly over time.",
        "We've eliminated memory leaks that previously caused weekly restarts.",
        "Heap dumps before and after show a 40% reduction in retained objects.",
        "The memory footprint stays constant even after days of uptime.",
        "This pattern prevents the gradual memory growth we saw earlier.",
        "GC pauses are now consistently under 10ms with this approach.",
        "Memory allocation patterns are much more predictable now.",
        "RSS memory usage stabilized at 800MB instead of growing to 4GB.",
        "GC cycles reduced from 200ms to 15ms pauses.",
        "Heap size stays under 2GB even after 30 days uptime.",
        "Memory leaks dropped from 50MB/hour to near zero.",
        "JVM heap tuning reduced full GC from hourly to weekly.",
        "Object pooling cut allocations by 60%.",
    ],
    "async": [
        "This enables handling thousands of concurrent connections.",
        "Non-blocking I/O here improves overall system throughput.",
        "The event loop efficiently manages multiple operations.",
        "This prevents thread blocking and improves responsiveness.",
        "It also reduces the need for large thread pools.",
        "We can now handle 50k+ websocket connections on a single instance.",
        "The non-blocking approach eliminates context switching overhead.",
        "Async processing allows the system to remain responsive during I/O waits.",
        "This pattern enables better resource utilization across the board.",
        "The callback mechanism ensures work continues without blocking.",
        "We've replaced thread-per-request with event-driven architecture successfully."
    ],
    "state": [
        "This ensures predictable state transitions.",
        "A single source of truth simplifies debugging considerably.",
        "State updates are tracked and auditable this way.",
        "This pattern prevents race conditions in state updates.",
        "It also makes it easier to reason about complex flows.",
        "The immutable state approach eliminates entire classes of bugs.",
        "We can replay state changes for debugging production issues.",
        "Time-travel debugging becomes possible with this architecture.",
        "State snapshots enable easy rollback and recovery.",
        "This centralized approach makes multi-tab synchronization trivial."
    ],
    "testing": [
        "Automated tests catch regressions early in the CI/CD pipeline.",
        "This improves code coverage and confidence before release.",
        "Mock objects help isolate units under test.",
        "Integration tests validate end-to-end workflows.",
        "A solid test suite here speeds up refactoring later.",
        "Our CI pipeline runs 5000+ tests in under 10 minutes.",
        "Test coverage increased from 45% to 85% with this strategy.",
        "Flaky tests were reduced to less than 1% using proper isolation.",
        "The test pyramid ensures fast feedback at all levels.",
        "Mutation testing verified the quality of our test suite.",
        "Contract tests prevent integration issues between services.",
        "Unit tests run in 30s, integration tests in 2min on CI.",
        "Code coverage went from 60% to 90% line coverage.",
        "E2E tests catch UI bugs before production deployment.",
        "Our CI/CD pipeline has 99% green build rate.",
        "TDD approach reduced bug density by 40%.",
        "Snapshot tests caught 50+ regression bugs.",
    ],
    "security": [
        "This mitigates common attack vectors like injection.",
        "Encryption at rest and in transit is essential.",
        "Input validation prevents malicious payloads from reaching core logic.",
        "Security audits often highlight this as a critical control.",
        "Combined with monitoring, this reduces the blast radius of incidents.",
        "We've hardened the system against OWASP Top 10 vulnerabilities.",
        "Penetration testing showed no critical findings after this fix.",
        "Rate limiting here prevents both abuse and DDoS attacks.",
        "The security layer operates with zero-trust principles.",
        "Least-privilege access reduces the impact of compromised credentials.",
        "Security headers and CSP policies provide defense in depth."
    ],
    "generic": [
        "This helps in reducing technical debt significantly.",
        "Tech debt decreased measurably after standardizing on this.",
        "It cuts down on maintenance burden over time.",
        "Long-term maintenance becomes much easier.",
        "Technical debt stops accumulating with this approach.",
        "Basically, it decouples the logic from the implementation.",
        "This design choice paid off once the system had to scale.",
        "The architecture review board approved this approach.",
        "The architecture becomes cleaner and more modular.",
        "It promotes separation of concerns effectively.",
        "The design follows SOLID principles naturally.",
        "I've used this pattern in previous projects with good results.",
        "We've seen fewer production incidents since adopting this.",
        "This has proven reliable across multiple projects.",
        "My experience shows this works consistently.",
        "We measured clear improvements after switching.",
        "Results speak for themselves with this approach.",
        "Code reviews consistently recommend this approach.",
        "The team adopted this after seeing clear benefits.",
        "Other teams in the company use this same pattern successfully.",
        "Cross-team collaboration improved with this shared pattern.",
        "Senior engineers mentor juniors to follow this pattern.",
        "The whole team got behind this quickly.",
        "Pair programming sessions reinforced this pattern.",
        "This aligns well with industry best practices.",
        "Documentation usually emphasizes this as the preferred method.",
        "This pattern is recommended in Martin Fowler's architecture guides.",
        "Official docs point to this as the best approach.",
        "It's documented extensively in the guides.",
        "The community has written a lot about this.",
        "Refactoring to this pattern improved maintainability.",
        "The codebase became significantly easier to onboard new developers.",
        "Code duplication dropped by 30% after this refactoring.",
        "Code quality metrics improved across the board.",
        "The refactor paid dividends immediately.",
        "Complexity metrics went down substantially.",
        "The approach simplifies both horizontal and vertical scaling.",
        "It scales well as requirements grow.",
        "We haven't hit scaling issues with this.",
        "Growth hasn't been a problem with this approach.",
        "Traffic spikes are handled gracefully.",
        "The design makes adding new features much faster now.",
        "New features ship quicker with this foundation.",
        "Development velocity increased noticeably.",
        "Iteration speed improved dramatically.",
        "Time-to-market for features dropped.",
        "Production stability improved after this change.",
        "We see fewer bugs related to this area.",
        "Error rates dropped after implementing this.",
        "System reliability went up measurably.",
        "Incidents related to this decreased.",
        "Unit tests became easier to write and maintain.",
        "Test coverage increased naturally with this approach.",
        "The code is much more testable now.",
        "Integration tests run faster with this structure.",
        "Mock objects are simpler with this design.",
        "Code readability improved significantly.",
        "New developers understand it faster.",
        "The intent is much clearer now.",
        "It's self-documenting to some extent.",
        "Less cognitive load for developers.",
        "Response times improved after optimization.",
        "CPU usage dropped noticeably.",
        "Memory footprint stayed stable.",
        "Throughput increased by measurable amounts.",
        "Latency decreased across the board.",
        "It's easy to extend when needed.",
        "Adding new behavior is straightforward.",
        "The system adapts to changes easily.",
        "Future requirements won't require rewrites.",
        "It's flexible enough for edge cases.",
        "Debugging became easier with this structure.",
        "Issues are isolated more quickly.",
        "Stack traces are clearer.",
        "Logs are more informative.",
        "Root cause analysis is simpler.",
        "Migration to this pattern was smooth.",
        "Adoption across the codebase went well.",
        "The rollout happened without issues.",
        "Teams picked it up quickly.",
        "Onboarding new engineers is faster.",
        "It's better than what we had before.",
        "The old approach had more problems.",
        "This beats the alternatives clearly.",
        "Compared to other options, this wins.",
        "Deployment frequency doubled.",
        "Mean time to recovery decreased.",
        "Change failure rate dropped.",
        "Lead time for changes improved.",
        "Build times got faster.",
        "API response time dropped to 50ms P95.",
        "DB query time went from 200ms to 20ms.",
        "SLA uptime improved to 99.95%.",
        "RPO decreased from 1 hour to 5 minutes.",
        "RTO improved from 30min to 2min.",
        "MTTR dropped from 2 hours to 15 minutes.",
        "MTBF increased from 30 days to 6 months.",
        "The CDN reduced bandwidth costs by 60%.",
        "TTL caching cut DB load by 70%.",
        "Load balancer distributes 100k RPS evenly.",
    ]
}

CLOSERS = [
    "So yeah, that's the gist of it.",
    "That more or less covers the basics.",
    "That's the main idea behind it.",
    "That sums it up pretty well.",
    "That's essentially what you need to know.",
    "So that's the overview.",
    "In a nutshell, that's it.",
    "That covers the essentials.",
    "That's the key takeaway.",
    "So that's how it works.",
    "That's how I would approach it.",
    "That's the way I think about it.",
    "That's my go-to method.",
    "That's how we handle it.",
    "That's how I'd solve it.",
    "That's the approach I take.",
    "That's what works for me.",
    "That's how we implement it in our codebase.",
    "That's our standard workflow.",
    "This is a common pattern in the industry.",
    "That's the standard approach most teams take.",
    "It's a fundamental part of the ecosystem.",
    "This is widely adopted in modern development.",
    "The ecosystem has standardized around this.",
    "It's become the de facto standard.",
    "Most companies follow this pattern.",
    "This is the industry norm now.",
    "That's the established pattern.",
    "It's a well-known best practice.",
    "In production, we handle this pretty carefully.",
    "You'll see this everywhere in production systems.",
    "It's battle-tested in production environments.",
    "This works reliably in production.",
    "We've used this in production for years.",
    "Real-world use cases confirm this works.",
    "This holds up in production scenarios.",
    "Production systems depend on this.",
    "Best practices generally suggest this direction.",
    "This aligns with engineering best practices.",
    "Documentation backs up this approach.",
    "The documentation explains this well.",
    "It's also well-documented in the official guides.",
    "This follows the recommended guidelines.",
    "That's what the docs recommend.",
    "That's been my experience with it so far.",
    "That's what I've learned from practice.",
    "This approach has served us quite well.",
    "I've seen this work consistently.",
    "That's what experience has taught me.",
    "This has proven reliable over time.",
    "We've had good results with this.",
    "This pattern scales well in practice.",
    "It scales horizontally without issues.",
    "Performance has been solid.",
    "This approach handles scale effectively.",
    "It performs well under load.",
    "Most developers I know follow this pattern.",
    "Teams typically converge on this solution.",
    "That's the modern consensus on this.",
    "The community has embraced this.",
    "This has widespread adoption.",
    "It's gaining traction in the community.",
    "Hope that makes sense.",
    "Does that answer your question?",
    "I think that explains it fairly well.",
    "It's pretty straightforward once you break it down.",
    "Hopefully that clarifies things.",
    "That should give you a good understanding.",
    "Does that clear things up?",
    "Is that clear enough?",
    "That should answer it.",
    "Let me know if you want more details.",
    "Happy to dive deeper if needed.",
    "I can elaborate if you want.",
    "Feel free to ask if unclear.",
    "Let me know if you need more context.",
    "I can explain further if that helps.",
    "It's basically the textbook solution for this.",
    "That's the canonical approach.",
    "This is Computer Science 101.",
    "It's the classic solution.",
    "This follows the standard algorithm.",
    "It's fairly simple in practice.",
    "Not too complicated once you get it.",
    "Pretty straightforward really.",
    "It's simpler than it sounds.",
    "Once you understand it, it's easy.",
    "The implementation is quite clean.",
    "Our team has standardized on this.",
    "This is what we agreed on as a team.",
    "The team converged on this solution.",
    "We've aligned on this approach.",
    "Everyone on the team uses this.",
    "This solves the problem effectively.",
    "It addresses the core issue.",
    "That's how you tackle this challenge.",
    "This is the solution that works.",
    "That handles the edge cases too.",
    "I'm confident this is the right approach.",
    "This has worked well for us.",
    "I trust this method.",
    "This is reliable.",
    "You can count on this approach.",
    "It beats the alternatives.",
    "Better than other options I've tried.",
    "This outperforms the alternatives.",
    "Compared to other methods, this wins.",
    "",
]

BAD_ANSWER_TEMPLATES = [
    "I don't know.",
    "I'm not sure.",
    "I don't really know about that.",
    "Honestly, I'm not familiar with that.",
    "I haven't worked with that before.",
    "I'm not confident about this topic.",
    "That's not something I know well.",
    "I'm drawing a blank here.",
    "I can't recall the details.",
    "I don't have experience with that.",
    "Um, I think {skill} might use {kw1}? Not really sure.",
    "Maybe it has something to do with {kw1}? I'm not certain.",
    "I believe {skill} does something with {kw1}, but I could be wrong.",
    "From what I've heard, {kw1} might be involved?",
    "It's probably related to {kw1}, but I'm not 100% sure.",
    "I think I've seen {kw1} mentioned somewhere in relation to {skill}.",
    "Could be {kw1}? Or maybe something else.",
    "Uh, {kw1} sounds familiar but I don't remember the details.",
    "Well, I mainly work with databases, so...",
    "I usually focus on the frontend side of things.",
    "That sounds like backend stuff, which isn't my area.",
    "I think that's more of a DevOps concern.",
    "We use different tools in my company.",
    "My team handles that differently.",
    "I'm more familiar with the opposite approach.",
    "{skill} handles this by handling it properly.",
    "You just need to use {skill} correctly.",
    "It works the way it's supposed to work.",
    "The important thing is to follow best practices.",
    "You implement it using standard implementation methods.",
    "It's done the normal way that things are done.",
    "You need to leverage the synergy between microservices and blockchain.",
    "It's all about cloud-native AI-driven optimization.",
    "We use agile methodologies to paradigm shift the infrastructure.",
    "The key is implementing serverless quantum computing patterns.",
    "You should utilize big data machine learning for scalability.",
    "{skill} is both synchronous and asynchronous at the same time.",
    "You need to avoid using {kw1}, which is why we always use {kw1}.",
    "It's important to never use caching, except when you cache everything.",
    "{skill} doesn't support that feature, but we use it all the time.",
    "It just works.",
    "You click the button.",
    "Install it and run it.",
    "Follow the tutorial.",
    "Read the documentation.",
    "Google it.",
    "Check Stack Overflow.",
    "Look at the examples.",
    "Use the default settings.",
    "Copy from the docs.",
    "Just run the command.",
    "Download and install.",
    "It's in the docs.",
    "Watch a YouTube tutorial.",
    "Ask ChatGPT.",
    "Search for it.",
    "Check the README.",
    "Use the template.",
    "It's a database, so you query it with HTTP.",
    "{skill} uses SQL queries to render the frontend.",
    "You deploy {skill} by compiling it to binary.",
    "Just use Docker to install {skill} on the database.",
]

VAGUE_ANSWER_TEMPLATES = [
    "{skill} is useful for building applications.",
    "{skill} helps developers create software.",
    "{skill} makes development easier.",
    "{skill} simplifies the development process.",
    "{skill} is good for building things.",
    "{skill} helps with software development.",
    "You use {kw1} to make things work better.",
    "{kw1} makes the application more efficient somehow.",
    "{kw1} improves the overall system.",
    "{kw1} helps with optimization.",
    "{kw1} makes things run smoother.",
    "{kw1} enhances the application.",
    "Using {kw1} improves performance.",
    "{kw1} makes the code better.",
    "I think {skill} uses {kw1} for performance reasons.",
    "The main idea is to use {kw1} properly.",
    "It's basically about using {kw1} in the right way.",
    "You need to implement {kw1} correctly.",
    "The key is understanding {kw1}.",
    "It involves working with {kw1}.",
    "{skill} is popular because it's easy to use.",
    "Many companies use {skill} for their projects.",
    "A lot of developers like {skill}.",
    "{skill} has gained popularity recently.",
    "The community loves {skill}.",
    "{skill} is widely adopted.",
    "{kw1} is important in {skill} development.",
    "You should know {kw1} when working with {skill}.",
    "{kw1} is essential for {skill}.",
    "Understanding {kw1} is crucial.",
    "{kw1} plays a key role.",
    "{kw1} is a core concept.",
    "{skill} provides tools for working with {kw1}.",
    "The framework handles {kw1} automatically.",
    "{skill} has built-in support for {kw1}.",
    "{skill} includes features for {kw1}.",
    "The library provides {kw1} functionality.",
    "{skill} offers {kw1} capabilities.",
    "Developers prefer {skill} because of {kw1}.",
    "Teams choose {skill} for {kw1}.",
    "Engineers like using {kw1} in {skill}.",
    "Most people use {kw1} with {skill}.",
    "{kw1} is a popular choice in {skill}.",
    "{skill} does what it's supposed to do.",
    "You use {kw1} when you need {kw1}.",
    "It works by working properly.",
    "{kw1} functions as intended.",
    "The system operates as expected.",
    "It's a matter of following best practices.",
    "You just need to implement it correctly.",
    "The documentation explains this.",
    "It's similar to other approaches.",
    "You handle it the standard way.",
    "It's done like you'd expect.",
    "I believe {kw1} is used for this.",
    "I think it involves {kw1}.",
    "It probably uses {kw1}.",
    "From what I remember, {kw1} is involved.",
    "If I recall correctly, {kw1} handles this.",
    "{kw1} is a {skill} feature.",
    "{skill} supports {kw1}.",
    "You can use {kw1} in {skill}.",
    "{kw1} exists in {skill}.",
    "{skill} has {kw1}.",
]

# ===== Per-question feedback templates =====

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

# ===== 2. HELPERS =====

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
    "How does {skill} handle {kw}?",
    "What is the role of {kw} in {skill}?",
    "Can you explain {kw} in the context of {skill}?",
    "What are the best practices for {subtopic_label} in {skill}?",
    "Tell me about {kw} in {skill}.",
    "What do you know about {kw} when working with {skill}?",
    "Explain how {kw} works inside {skill}.",
    "Describe how you would use {kw} when working with {skill}.",
    "Explain the relationship between {kw} and {subtopic_label} in {skill}.",
    "Could you walk me through {kw} in {skill}?",
    "How would you explain {kw} to someone new to {skill}?",
    "Describe the implementation of {kw} in a typical {skill} project.",
    "Why is {kw} important when building systems with {skill}?",
    "Why does {subtopic_label} matter when using {skill} in production?",
    "What makes {kw} necessary in {skill} development?",
    "Why do developers choose {kw} when working with {skill}?",
    "Imagine you are building a production system with {skill}. How would you approach {kw}?",
    "Suppose you have a problem related to {kw} in {skill}. What would you do?",
    "If a bug appears around {kw} in {skill}, how would you debug it?",
    "You're starting a new {skill} project. How would you set up {kw}?",
    "If you were optimizing {kw} in an existing {skill} application, where would you start?",
    "Let's say your {skill} system is slow because of {kw}. What would you investigate?",
    "What are common pitfalls related to {kw} when using {skill}?",
    "What best practices do you follow for {subtopic_label} in {skill}?",
    "What mistakes do developers typically make with {kw} in {skill}?",
    "How do you ensure {kw} is implemented correctly in {skill}?",
    "What should be avoided when working with {kw} in {skill}?",
    "Walk me through how you would design {subtopic_label} using {skill}.",
    "How would you integrate {kw} into a larger architecture built on {skill}?",
    "How does {kw} fit into the overall architecture of a {skill} application?",
    "What architectural considerations are important for {kw} in {skill}?",
    "How would you structure a {skill} project to best utilize {kw}?",
    "How does {kw} in {skill} compare to alternative approaches or tools?",
    "When would you avoid using {kw} in {skill}, and why?",
    "What are the alternatives to {kw} in {skill}?",
    "How does {skill}'s approach to {kw} differ from other frameworks?",
    "In what situations is {kw} better than other options in {skill}?",
    "In your experience, what are the key challenges around {kw} in {skill}?",
    "Can you share a situation where {kw} in {skill} made a big difference to your system?",
    "Have you ever had issues with {kw} in {skill}? How did you resolve them?",
    "What have you learned about {kw} from using {skill} in production?",
    "Tell me about a time when understanding {kw} helped you solve a problem in {skill}.",
    "How do you think about trade-offs when using {kw} in {skill}?",
    "If you had to redesign a feature around {kw} in {skill}, what would you change?",
    "What are the pros and cons of using {kw} in {skill}?",
    "When would you choose {kw} over other approaches in {skill}?",
    "How do you balance {kw} with other concerns in {skill} development?",
    "What happens under the hood when {skill} uses {kw}?",
    "Can you explain the internal mechanism of {kw} in {skill}?",
    "How is {kw} implemented internally in {skill}?",
    "What's the difference between basic and advanced usage of {kw} in {skill}?",
    "How would you troubleshoot issues related to {kw} in {skill}?",
    "What tools would you use to monitor {kw} in a {skill} application?",
    "How do you test {kw} functionality in {skill}?",
    "What metrics would you track for {kw} in {skill}?",
]

FOLLOWUP_TEMPLATES = [
    "Earlier you mentioned **{kw}** in {skill}. Could you expand on what you meant by that?",
    "You brought up **{kw}**. Can you go deeper into how it actually works under the hood?",
    "You briefly mentioned **{kw}** — can you explain its role in {skill} in more detail?",
    "Can you clarify how **{kw}** interacts with other components when using {skill}?",
    "You said **{kw}** is important. Why exactly is it critical in real systems?",
    "I'd like to understand **{kw}** better. Can you elaborate on that?",
    "That's interesting about **{kw}**. What makes it work that way in {skill}?",
    "You touched on **{kw}**. Could you dive deeper into the technical details?",
    "Can you explain more about how **{kw}** functions in {skill}?",
    "What exactly do you mean when you say **{kw}** in the context of {skill}?",
    "What are the trade-offs of using **{kw}** in a production environment with {skill}?",
    "Is there any situation where you would avoid using **{kw}** in {skill}? Why?",
    "How do you decide between using **{kw}** and another alternative in {skill}?",
    "You seem confident about **{kw}** — what would you consider its biggest limitation?",
    "If **{kw}** becomes a bottleneck, how would you approach solving that?",
    "What are the downsides of relying on **{kw}** in {skill}?",
    "When might **{kw}** not be the best choice in {skill}?",
    "How do you weigh the benefits against the costs of using **{kw}**?",
    "What alternatives to **{kw}** have you considered in {skill} projects?",
    "Imagine **{kw}** suddenly behaves incorrectly in production. How would you debug that?",
    "If a critical incident happens related to **{kw}**, what steps would you take to investigate?",
    "What metrics or logs would you look at to troubleshoot issues around **{kw}**?",
    "How would you diagnose a performance problem with **{kw}** in {skill}?",
    "What would be your first step if **{kw}** started causing errors?",
    "If **{kw}** is failing intermittently, how would you track down the root cause?",
    "What monitoring would you put in place for **{kw}** in production?",
    "Suppose your system needs to scale quickly. How does **{kw}** affect your design choices?",
    "If you had to refactor code that heavily uses **{kw}**, how would you approach it?",
    "How would **{kw}** behave under extreme load, and what would you watch out for?",
    "Let's say traffic suddenly doubles. How does **{kw}** impact your {skill} system?",
    "If you needed to migrate away from **{kw}**, what would your strategy be?",
    "How would you optimize **{kw}** if performance became critical?",
    "What would happen if **{kw}** failed completely in your system?",
    "You mentioned **{kw}**. How does it compare to other patterns or tools solving the same problem?",
    "What would be a reasonable alternative to **{kw}**, and when would it be better?",
    "What mistakes do engineers commonly make when using **{kw}** in {skill}?",
    "How does **{kw}** in {skill} differ from similar concepts in other frameworks?",
    "What advantages does **{kw}** have over traditional approaches?",
    "Are there situations where simpler alternatives to **{kw}** would be preferable?",
    "Why do you think **{kw}** is the right approach here instead of other options?",
    "What underlying principle in {skill} makes **{kw}** a good fit?",
    "If you were reviewing someone else's code using **{kw}**, what would you look for?",
    "What would make you choose **{kw}** over other solutions in {skill}?",
    "How did you decide that **{kw}** was the appropriate technique to use?",
    "What factors influence whether to use **{kw}** in a given situation?",
    "Have you faced real issues with **{kw}** before? What happened and what did you learn?",
    "Can you share a real-world scenario where **{kw}** significantly improved or degraded system behavior?",
    "Which part of **{kw}** do you still find tricky or easy to get wrong?",
    "In your projects, how often have you actually needed to use **{kw}**?",
    "What lessons have you learned from working with **{kw}** in production?",
    "Have you seen **{kw}** cause problems in production systems before?",
    "What's the most challenging aspect of implementing **{kw}** correctly?",
    "How would you actually implement **{kw}** in a {skill} application?",
    "What does the code look like when you use **{kw}** in {skill}?",
    "Are there specific configuration options for **{kw}** you'd recommend?",
    "What dependencies or setup does **{kw}** require in {skill}?",
    "What best practices should teams follow when using **{kw}**?",
    "How do you ensure **{kw}** is used consistently across a codebase?",
    "What documentation or guidelines would you create for **{kw}**?",
    "How do you teach junior developers to use **{kw}** properly in {skill}?",
]

def pretty_subtopic(skill, subtopic):
    if skill not in SKILL_DATA or skill == "Generic":
        label = GENERIC_SUBTOPIC_LABELS.get(subtopic, subtopic)
    else:
        label = subtopic.replace("_", " ")
    return normalize_subtopic_label(label)

def get_skill_keywords(skill, subtopic=None):
    data = SKILL_DATA.get(skill, SKILL_DATA["Generic"])
    if subtopic and subtopic in data:
        return data[subtopic]
    all_kws = []
    for _, v in data.items():
        all_kws.extend(v)
    return all_kws or ["core concepts"]

def add_noise(text):
    if random.random() > 0.7:
        return text

    if random.random() > 0.6:
        text = text.lower()

    if random.random() > 0.7 and len(text.split()) > 10:
        fillers = [
            "um,", "uh,", "like,", "you know,",
            "sort of,", "kind of,", "I mean,", "basically,",
            "actually,", "essentially,", "pretty much,", "more or less,",
            "so to speak,", "if you will,", "as it were,",
            "in a way,", "in some sense,", "to some extent,",
            "I guess,", "I suppose,", "I think,", "I believe,",
            "probably,", "maybe,", "perhaps,",
            "well,", "so,", "anyway,", "right,",
        ]
        words = text.split()
        possible_positions = [i for i, w in enumerate(words) if w.endswith(",") or w.endswith(".")]
        if possible_positions:
            insert_pos = random.choice(possible_positions) + 1
            if insert_pos < len(words):
                words.insert(insert_pos, random.choice(fillers))
                text = " ".join(words)

    if text.endswith(".") and random.random() > 0.7:
        text = text[:-1]

    return text

def sample_items(pool, min_k, max_k):
    if not pool:
        return []
    max_k_eff = min(max_k, len(pool))
    min_k_eff = min(min_k, max_k_eff)
    if max_k_eff <= 0:
        return []
    k = random.randint(min_k_eff, max_k_eff)
    return random.sample(pool, k=k)

def clamp01(x: float) -> float:
    return max(0.0, min(1.0, x))

def normalize_seniority(raw: str) -> str:
    s = (raw or "").strip().lower()
    if "intern" in s:
        return "Intern"
    if "junior" in s:
        return "Junior"
    if "senior" in s and "lead" not in s:
        return "Senior"
    if "lead" in s or "principal" in s or "staff" in s:
        return "Lead"
    # default
    return "Junior"


def map_score_to_overview(avg: float) -> str:
    """
    Map điểm trung bình (0–1) -> overview band.

    Ngưỡng mới (dễ đọc hơn):
      < 0.40  -> POOR
      < 0.50  -> BELOW AVERAGE
      < 0.65  -> AVERAGE
      < 0.80  -> GOOD
      >= 0.80 -> EXCELLENT
    """
    if avg < 0.40:
        return "POOR"
    if avg < 0.50:
        return "BELOW AVERAGE"
    if avg < 0.65:
        return "AVERAGE"
    if avg < 0.80:
        return "GOOD"
    return "EXCELLENT"

def mix_sentence_parts(parts, quality):
    parts = [p for p in parts if p and p.strip()]
    if not parts:
        return ""
    if len(parts) == 1:
        return parts[0]

    if quality == "high":
        connectors = [
            ", so", ", thus", ", therefore", ", hence",
            ". As a result", ". Consequently", ". This means that",
            ", which means", ", resulting in", ", leading to",
            ". Additionally", ". Furthermore", ". Moreover",
            ". Also", ". Plus", ". In addition",
            ", and this", ", and also", ", plus",
            ". However", ". On the other hand", ". That said",
            ", though", ", although", ", while",
            ". In contrast", ". Alternatively",
            ". In other words", ". That is", ". Specifically",
            ". To be more precise", ". More specifically",
            ", meaning", ", which", ", that",
            ". Subsequently", ". Then", ". After that",
            ". Following this", ". Next",
            ". Indeed", ". In fact", ". Notably",
            ". Importantly", ". Significantly",
            ". In practice", ". Practically speaking", ". In real-world terms",
            ". When applied", ". In production",
            ", allowing", ", enabling", ", facilitating",
            ", which allows", ", which enables",
            ". For instance", ". For example", ". As an example",
            ". Ultimately", ". In the end", ". Overall",
        ]
    elif quality == "avg":
        connectors = [
            ", and", ", but", ", or", ", so",
            ". Also", ". Then", ". Plus",
            ", which", ", that", ", where",
            ". This", ". It", ". That",
            ". And", ". But", ". Or",
            ", because", ", since",
        ]
    else:
        connectors = [". ", " ", ", ", "", "  "]

    result = parts[0].rstrip('.').rstrip(',').rstrip()

    for i in range(1, len(parts)):
        part = parts[i].strip()
        transition_prefixes = ["This approach", "Additionally", "Furthermore", "In practice", "As a result", "Also", "Then"]
        for prefix in transition_prefixes:
            if part.startswith(prefix):
                part = part[len(prefix):].lstrip(",").lstrip()
                break
        connector = random.choice(connectors)
        result = result.rstrip('.').rstrip(',').rstrip()
        if connector.startswith("."):
            result += ". " + connector.strip(". ") + " " + part
        elif connector.startswith(","):
            result += connector + " " + part
        else:
            if connector.strip():
                result += " " + connector.strip() + " " + part
            else:
                result += " " + part

    result = result.replace("  ", " ").replace(" ,", ",").replace(" .", ".")
    if result and not result.endswith((".", "!", "?")):
        result += "."
    return result

def add_version_specific_info(skill):
    version_info = {
        "Python": ["Python 3.11+", "Python 3.10", "Python 3.12"],
        "Node.js": ["Node 18 LTS", "Node 20", "Node.js 16+"],
        "React": ["React 18", "React 17+", "React 19 beta"],
        "TypeScript": ["TypeScript 5.0", "TS 4.9+", "TypeScript 5.2"],
        "Java": ["Java 17 LTS", "Java 21", "JDK 11+"],
        "Go": ["Go 1.21", "Go 1.20+", "Go 1.22"],
        "Kubernetes": ["K8s 1.28", "Kubernetes 1.27+", "K8s 1.29"],
        "Docker": ["Docker 24.0", "Docker 20+"],
        "PostgreSQL": ["Postgres 15", "PostgreSQL 14+", "Postgres 16"],
        "Redis": ["Redis 7.0", "Redis 6+"],
    }
    if skill in version_info:
        version = random.choice(version_info[skill])
        phrases = [
            f"Since {version}, this has been much more stable",
            f"{version} introduced significant improvements here",
            f"We upgraded to {version} specifically for this feature",
            f"{version} has native support for this",
            f"This works best in {version} and above",
        ]
        return random.choice(phrases)
    return ""

def add_contextual_detail(skill, kw, quality):
    if quality == "high":
        detail_types = [
            f"In production, {kw} helps handle 10k+ requests/second efficiently",
            f"The {kw} pattern reduced our latency from 500ms to under 50ms",
            f"Most {skill} developers rely on {kw} for 99.9% uptime",
            f"{kw} integrates seamlessly with modern {skill} workflows",
            f"From an architectural standpoint, {kw} provides flexibility",
            f"Performance benchmarks show {kw} reduces latency by 60-70%",
            f"The community consensus favors {kw} for this use case",
            f"Security audits typically highlight {kw} as a best practice",
            f"We measured 3x throughput improvement with {kw}",
            f"Memory usage dropped 40% after implementing {kw}",
            f"The {kw} approach handles 100k concurrent connections",
            f"CPU utilization stays under 50% with {kw}",
            f"Response time decreased from 2s to 200ms using {kw}",
            f"Our P99 latency is now under 100ms thanks to {kw}",
            f"Database queries went from N+1 to batched with {kw}",
            f"We scaled from 1k to 50k users using {kw}",
        ]
        if random.random() > 0.8:
            version_detail = add_version_specific_info(skill)
            if version_detail:
                return version_detail
        return random.choice(detail_types)
    elif quality == "avg":
        detail_types = [
            f"{kw} is commonly used in {skill}",
            f"Many projects use {kw}",
            f"{kw} works well for this",
            f"I've seen {kw} in action before",
            f"Teams typically implement {kw}",
            f"The docs recommend {kw}",
        ]
        return random.choice(detail_types)
    else:
        return ""

def generate_answer_with_variation(skill, quality="high", subtopic="generic", target_score=None):
    keywords = get_skill_keywords(skill, subtopic)
    kw1 = random.choice(keywords)
    kw2 = random.choice(keywords)
    templates = CORE_TEMPLATES.get(subtopic, CORE_TEMPLATES["generic"])
    detail_list = DETAILS.get(subtopic, DETAILS["generic"])

    if quality == "high":
        if target_score and target_score >= 0.90:
            structure_weights = [0.2, 0.2, 0.2, 0.4]
        else:
            structure_weights = [0.4, 0.3, 0.2, 0.1]

        structure_type = random.choices(
            ["detailed", "technical", "practical", "comprehensive"],
            weights=structure_weights
        )[0]

        if structure_type == "detailed":
            opener = random.choice(OPENERS)
            core = random.choice(templates).format(skill=skill, kw1=kw1, kw2=kw2)
            detail = random.choice(detail_list)
            closer = random.choice(CLOSERS)
            parts = [opener, core, detail, closer]

        elif structure_type == "technical":
            core = random.choice(templates).format(skill=skill, kw1=kw1, kw2=kw2)
            detail = random.choice(detail_list)
            context = add_contextual_detail(skill, kw1, quality)
            parts = [core, detail]
            if context:
                parts.append(context)

        elif structure_type == "practical":
            opener = random.choice(OPENERS)
            core = random.choice(templates).format(skill=skill, kw1=kw1, kw2=kw2)
            detail1 = random.choice(detail_list)
            detail2 = random.choice(detail_list)
            parts = [opener, core, detail1, detail2] if random.random() > 0.5 else [core, detail1, detail2]

        else:  # comprehensive
            opener = random.choice(OPENERS)
            core1 = random.choice(templates).format(skill=skill, kw1=kw1, kw2=kw2)
            core2 = random.choice(templates).format(skill=skill, kw1=kw2, kw2=kw1)
            detail = random.choice(detail_list)
            closer = random.choice(CLOSERS)
            parts = [opener, core1, core2, detail, closer]

        raw_text = mix_sentence_parts(parts, "high")

    elif quality == "avg":
        structure_type = random.choice(["vague", "incomplete", "basic"])
        if structure_type == "vague":
            vague = random.choice(VAGUE_ANSWER_TEMPLATES)
            opener = random.choice(OPENERS) if random.random() > 0.5 else ""
            core = vague.format(skill=skill, kw1=kw1, kw2=kw2)
            parts = [opener, core] if opener else [core]
        elif structure_type == "incomplete":
            opener = random.choice(OPENERS) if random.random() > 0.6 else ""
            core = random.choice(templates).format(skill=skill, kw1=kw1, kw2=kw2)
            parts = [opener, core] if opener else [core]
        else:
            core = random.choice(templates).format(skill=skill, kw1=kw1, kw2=kw2)
            detail = random.choice(detail_list) if random.random() > 0.5 else ""
            parts = [core, detail] if detail else [core]
        raw_text = mix_sentence_parts(parts, "avg")

    elif quality == "low":
        bad_type = random.choice(["template_bad", "confused", "minimal"])
        if bad_type == "template_bad":
            bad_tmpl = random.choice(BAD_ANSWER_TEMPLATES)
            if "{skill}" in bad_tmpl or "{kw1}" in bad_tmpl:
                raw_text = bad_tmpl.format(skill=skill, kw1=kw1, kw2=kw2, subtopic=subtopic)
            else:
                raw_text = bad_tmpl
        elif bad_type == "confused":
            wrong_kw = random.choice(["blockchain", "quantum computing", "AI", "machine learning", "big data"])
            parts = [
                f"{skill} uses {wrong_kw}",
                random.choice(VAGUE_ANSWER_TEMPLATES).format(skill=skill, kw1=kw1, kw2=kw2)
            ]
            raw_text = ". ".join(parts)
        else:
            minimal_options = [
                f"{skill} handles this.",
                f"Just use {kw1}.",
                f"It works with {skill}.",
                "Follow the documentation.",
            ]
            raw_text = random.choice(minimal_options)
    else:
        very_poor_options = [
            "I don't know.",
            "I'm not sure about that.",
            "I haven't worked with that.",
            "That's not my area of expertise.",
            "I can't answer that question.",
            "Hmm, I'm not familiar with this.",
            "Sorry, I don't have experience with this.",
            "I'm drawing a blank here.",
            "uh, I'm not really sure",
            "I think... actually no, I don't know",
        ]
        raw_text = random.choice(very_poor_options)

    return add_noise(raw_text)

def generate_question(skill, subtopic="generic"):
    keywords = get_skill_keywords(skill, subtopic)
    kw = random.choice(keywords)
    subtopic_label = pretty_subtopic(skill, subtopic)
    template = random.choice(QUESTION_TEMPLATES)
    return template.format(skill=skill, kw=kw, subtopic_label=subtopic_label)

# ===== 3. OVERALL FEEDBACK POOLS =====

# ===== Overall Assessment Templates =====

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
        # UPDATED: bỏ chữ 'developer'
        "The candidate meets expectations for a {seniority} {role} and shows good potential to grow further.",
        "They demonstrated comfort discussing common patterns and real-world situations related to {skill}.",
        "Their ability to articulate technical decisions was good but can improve with more hands-on production exposure.",
        "Communication was structured, albeit occasionally too high-level.",
        "With focused effort on advanced concepts, they could reach an excellent performance band.",
        "They showed consistency across questions, indicating steady foundational strength."
    ],
    "AVERAGE": [
        "The candidate demonstrated **partial understanding** of {skill}, particularly related to **{subtopic_label}**.",
        "They delivered mixed performance across {num_questions} questions, resulting in an overall trend around **{pct}%**.",
        "They recognized key terms but struggled to provide precise or complete explanations.",
        "Their answers tended to remain conceptual rather than practical or implementation-focused.",
        "They occasionally gave correct ideas but without connecting them to real-world engineering scenarios.",
        "Communication was acceptable but sometimes disorganized, affecting clarity.",
        "They showed potential but need substantial improvement in applying concepts in depth.",
        "They lacked confidence when asked to justify reasoning or compare alternatives.",
        "Their understanding appears based more on theory or surface learning than practical experience.",
        "Several areas of {subtopic_label} were addressed only partially or inaccurately.",
        "Overall, the candidate meets baseline requirements but is not yet fully ready for advanced tasks expected at this level."
    ],
    "BELOW AVERAGE": [
        "The candidate showed **limited understanding** of {skill} and struggled with questions about **{subtopic_label}**.",
        "Their performance across {num_questions} questions resulted in a below-expectation score around **{pct}%**.",
        "Answers commonly lacked detail, accuracy, or relevance to the question.",
        "They had difficulty explaining foundational concepts or relating them to practical usage.",
        "Communication lacked structure, making reasoning hard to follow.",
        "They appeared unfamiliar with common tools, patterns, and workflows associated with {skill}.",
        "Many explanations were vague and showed signs of guesswork rather than understanding.",
        "They struggled to answer follow-up questions or go beyond surface definitions.",
        "Their familiarity with {subtopic_label} appears minimal, with frequent confusion about terminology.",
        # UPDATED: bỏ chữ 'developer'
        "At this time, they **do not fully meet** expectations for a {seniority} {role}.",
        "Focused upskilling is needed before they can handle core responsibilities confidently."
    ],
    "POOR": [
        "The candidate demonstrated **major gaps in fundamental knowledge** of {skill}, especially regarding **{subtopic_label}**.",
        "Their overall score of **{pct}%** indicates insufficient readiness for a {seniority} {role}.",
        "They were unable to explain essential concepts or articulate how technologies fit together.",
        "Most responses lacked clarity, correctness, and connection to real-world engineering.",
        "They showed confusion and frequently contradicted themselves while discussing basic topics.",
        "Their difficulty answering follow-up questions suggests a lack of foundational understanding.",
        "They demonstrated little awareness of practical usage or industry best practices.",
        "Communication issues further obscured their technical thinking.",
        "They would benefit significantly from guided learning and structured hands-on practice before reattempting.",
        "A focused learning journey on fundamentals is strongly recommended at this stage."
    ]
}

# ===== Strength / Weakness / Recommendation Pools =====

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
        # UPDATED: tránh lỗi số ít/số nhiều
        "Understands how {subtopic_label} show up in everyday engineering work.",
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
        # UPDATED: tránh lặp '{subtopic_label} patterns' khi subtopic đã chứa 'patterns'
        "Still developing deeper intuition for advanced scenarios in {subtopic_label}.",
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

# ===== Seniority-specific tone additions =====

SENIORITY_ASSESSMENT_NOTES = {
    "Intern": [
        "For an intern-level profile, this is a very promising performance that provides a strong starting point for your early career.",
        "At intern level, the focus should be on building confidence and fundamentals, and this session reflects meaningful progress in that direction."
    ],
    "Junior": [
        "For a junior engineer, this performance shows a solid base to grow from, especially if you keep practicing consistently.",
        "At junior level, the key is to convert this knowledge into more hands-on experience, which your answers suggest you are ready for."
    ],
    "Senior": [
        "For a senior engineer, the next step is to consistently connect these insights to broader system ownership and mentoring responsibilities.",
        "At senior level, refining communication and decision-making around trade-offs will further amplify your impact."
    ],
    "Lead": [
        "For a lead-level role, it will be important to continuously frame these technical choices in terms of team alignment and long-term strategy.",
        "As a lead, scaling your thinking across teams and guiding others through these trade-offs will be a key success factor."
    ],
}

SENIORITY_RECOMMENDATION_EXTRA = {
    "Intern": [
        "As an intern, treat this feedback as a roadmap: focus on one or two key skills at a time, and celebrate small, consistent improvements.",
        "At this stage, it is completely normal to have gaps — the most important thing is maintaining curiosity and turning each interview into a learning opportunity."
    ],
    "Junior": [
        "As a junior engineer, aim to transform theoretical knowledge into practical experience by building small but real projects and asking for feedback regularly.",
        "Staying intentional about learning, documenting what you practice, and seeking mentorship will accelerate your growth significantly at this level."
    ],
    "Senior": [
        "As a senior engineer, focus on sharpening your ability to reason about trade-offs clearly and guide others through complex decisions.",
        "Investing in clearer communication, mentoring peers, and owning critical areas end-to-end will help you operate effectively at senior level."
    ],
    "Lead": [
        "As a lead, use this feedback to align your technical decisions with product and people outcomes, not just code quality.",
        "Center your development around enabling the team: unblocking others, setting standards, and communicating direction with clarity and calmness."
    ],
}

# ===== Helper: chuẩn hóa subtopic_label (tránh 'core' bị trơ =====

def normalize_subtopic_label(raw: str) -> str:
    """
    Chuẩn hóa subtopic_label trước khi format template,
    ví dụ 'core' / 'general' -> 'core concepts'.
    """
    if not raw:
        return "core concepts"
    cleaned = raw.strip().lower()
    if cleaned in {"core", "general"}:
        return "core concepts"
    return raw


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
    base_text = " ".join(s.format(**ctx) for s in sentences)

    # ===== Thêm closing sentence phù hợp seniority + overview =====
    norm = normalize_seniority(seniority)

    if norm == "Intern":
        if overview in ("EXCELLENT", "GOOD"):
            closing = (
                f"For an intern-level {role}, scoring around **{pct}%** over {num_questions} questions "
                "shows a very promising starting point. With structured practice and mentorship, "
                "you can grow quickly into more challenging responsibilities."
            )
        elif overview == "AVERAGE":
            closing = (
                f"For an intern-level {role}, a score around **{pct}%** indicates a workable base to build on. "
                "The main focus now should be strengthening fundamentals and turning theory into hands-on practice."
            )
        elif overview == "BELOW AVERAGE":
            closing = (
                f"For an intern-level {role}, roughly **{pct}%** reflects an early-stage profile with clear gaps. "
                "With patience, clear learning goals, and consistent effort, you can still turn this into a solid foundation."
            )
        else:  # POOR
            closing = (
                f"For an intern-level {role}, a score near **{pct}%** signals that this is still a very early starting point. "
                "At this stage, priority is to build basic confidence through small, guided projects and fundamental exercises."
            )

    elif norm == "Junior":
        if overview in ("EXCELLENT", "GOOD"):
            closing = (
                f"For a junior {role}, achieving around **{pct}%** over {num_questions} questions meets and in some areas "
                "exceeds expectations. With more exposure to production systems, you can move toward senior-level impact."
            )
        elif overview == "AVERAGE":
            closing = (
                f"For a junior {role}, a score around **{pct}%** is acceptable but leaves clear room for growth. "
                "Continued practice on real-world tasks and frequent feedback loops will be essential."
            )
        elif overview == "BELOW AVERAGE":
            closing = (
                f"For a junior {role}, roughly **{pct}%** indicates that fundamentals in {skill} need more attention "
                "before taking on complex ownership. Focusing on a few core areas at a time will make progress more sustainable."
            )
        else:  # POOR
            closing = (
                f"For a junior {role}, a score near **{pct}%** is noticeably below expectations. "
                "A reset on the basics and a structured learning path will be required before you can contribute reliably "
                "in a production environment."
            )

    elif norm == "Senior":
        if overview in ("EXCELLENT", "GOOD"):
            closing = (
                f"For a senior {role}, scoring about **{pct}%** over {num_questions} questions aligns with expectations. "
                "The next step is to keep translating this depth into system ownership, mentoring, and long-term decisions."
            )
        elif overview == "AVERAGE":
            closing = (
                f"For a senior {role}, a score of roughly **{pct}%** is somewhat below the depth typically expected. "
                "You will need to strengthen certain areas to consistently lead design and implementation efforts."
            )
        elif overview == "BELOW AVERAGE":
            closing = (
                f"For a senior {role}, around **{pct}%** indicates a noticeable gap compared to role expectations. "
                "Bridging this requires deliberate work on fundamentals and more structured involvement in complex projects."
            )
        else:  # POOR
            closing = (
                f"For a senior {role}, a score near **{pct}%** is significantly below the bar for independent ownership. "
                "Substantial upskilling is needed before you can reliably lead critical initiatives."
            )

    else:  # Lead
        if overview in ("EXCELLENT", "GOOD"):
            closing = (
                f"As a lead {role}, scoring around **{pct}%** over {num_questions} questions shows that you have "
                "the technical foundation to guide others. The key is to consistently scale this thinking across teams "
                "through clear direction, standards, and mentoring."
            )
        elif overview == "AVERAGE":
            closing = (
                f"As a lead {role}, a score of roughly **{pct}%** suggests that while some experience is present, there are gaps "
                "that may limit your ability to set technical direction confidently. Focusing on deepening expertise in {skill} "
                "will be important."
            )
        elif overview == "BELOW AVERAGE":
            closing = (
                f"As a lead {role}, scoring around **{pct}%** is below expectations for a role that shapes technical strategy. "
                "You should prioritize reinforcing core concepts and decision-making skills before taking on heavy leadership "
                "responsibilities."
            )
        else:  # POOR
            closing = (
                f"As a lead {role}, a score near **{pct}%** highlights a serious mismatch between current capabilities "
                "and role expectations. A step back into more hands-on, learning-focused work may be necessary before "
                "resuming lead-level responsibilities."
            )

    return base_text + " " + closing


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
    base = " ".join(s.format(**ctx) for s in sentences)
    # Thêm coaching sentence riêng cho từng seniority
    extra_pool = SENIORITY_RECOMMENDATION_EXTRA.get(seniority, [])
    if extra_pool:
        extra = random.choice(extra_pool)
        base = base + " " + extra
    return base

# ===== 4. SESSION BUILDER =====

def create_session():
    role = random.choice(ROLES)
    seniority = random.choice(["Intern", "Junior", "Senior", "Lead"])
    skill = random.choice(SKILLS)

    skill_config = SKILL_DATA.get(skill, SKILL_DATA["Generic"])
    skill_subtopics = list(skill_config.keys())

    num_main_subtopics = random.choices([1, 2], weights=[0.6, 0.4])[0]
    main_subtopics = random.sample(skill_subtopics, k=min(num_main_subtopics, len(skill_subtopics)))
    current_subtopic = main_subtopics[0]

    strong_subtopics = []
    weak_subtopics = []
    if len(skill_subtopics) >= 2:
        strong_subtopics = [random.choice(skill_subtopics)]
        remaining = [s for s in skill_subtopics if s not in strong_subtopics]
        if remaining:
            weak_subtopics = [random.choice(remaining)]

    num_questions = random.choices([3, 4, 5], weights=[0.5, 0.4, 0.1])[0]
    candidate_tier = random.choices(
        ["Excellent", "Good", "Average", "Weak", "Very Poor"],
        weights=[0.15, 0.30, 0.30, 0.15, 0.10]
    )[0]

    conversation = []
    all_scores = []
    prev_keywords = []

    for i in range(1, num_questions + 1):
        is_follow_up = False
        if i > 1 and prev_keywords:
            prev_score = all_scores[-1] if all_scores else 0.5
            if 0.50 < prev_score < 0.85:
                is_follow_up = random.random() > 0.5
            elif prev_score >= 0.85:
                is_follow_up = random.random() > 0.7
            elif prev_score < 0.30:
                is_follow_up = random.random() > 0.85

        if is_follow_up and prev_keywords:
            kw = random.choice(prev_keywords)
            subtopic_label = pretty_subtopic(skill, current_subtopic)
            template = random.choice(FOLLOWUP_TEMPLATES)
            question = template.format(skill=skill, kw=kw, subtopic_label=subtopic_label)
        else:
            if random.random() < 0.7 and main_subtopics:
                current_subtopic = random.choice(main_subtopics)
            else:
                current_subtopic = random.choice(skill_subtopics)
            question = generate_question(skill, current_subtopic)

        current_keywords = get_skill_keywords(skill, current_subtopic)
        if current_keywords:
            prev_keywords = random.sample(current_keywords, k=min(3, len(current_keywords)))

        base_quality_dist = {
            "Excellent": (["high", "avg"], [0.95, 0.05]),
            "Good": (["high", "avg", "low"], [0.6, 0.35, 0.05]),
            "Average": (["high", "avg", "low"], [0.15, 0.70, 0.15]),
            "Weak": (["avg", "low", "very_poor"], [0.25, 0.65, 0.10]),
            "Very Poor": (["low", "very_poor"], [0.20, 0.80])
        }
        qualities, weights = base_quality_dist[candidate_tier]
        q_qual = random.choices(qualities, weights=weights)[0]

        if current_subtopic in strong_subtopics and candidate_tier not in ["Excellent"]:
            if q_qual == "low":
                q_qual = "avg" if random.random() > 0.5 else "low"
            elif q_qual == "avg":
                q_qual = "high" if random.random() > 0.4 else "avg"
            elif q_qual == "very_poor":
                q_qual = "low" if random.random() > 0.6 else "very_poor"
        elif current_subtopic in weak_subtopics and candidate_tier not in ["Very Poor"]:
            if q_qual == "high":
                q_qual = "avg" if random.random() > 0.5 else "high"
            elif q_qual == "avg":
                q_qual = "low" if random.random() > 0.4 else "avg"

        if q_qual == "high":
            base = 0.88
            variation = 0.10
        elif q_qual == "avg":
            base = 0.58
            variation = 0.10
        elif q_qual == "low":
            base = 0.25
            variation = 0.10
        else:
            base = 0.08
            variation = 0.10

        expected_score = base + random.uniform(-variation, variation)
        answer = generate_answer_with_variation(skill, q_qual, current_subtopic, target_score=expected_score)

        score_adjustment = 0.0
        answer_length = len(answer.split())
        if answer_length < 10 and q_qual in ["high", "avg"]:
            score_adjustment -= 0.10
        elif answer_length > 40 and q_qual == "high":
            score_adjustment += 0.05
        elif answer_length < 5:
            score_adjustment -= 0.15

        uncertainty_words = ["i think", "maybe", "not sure", "i don't know", "um", "uh", "probably", "i believe"]
        uncertainty_count = sum(1 for word in uncertainty_words if word in answer.lower())
        if uncertainty_count > 0:
            score_adjustment -= min(0.15, uncertainty_count * 0.05)

        strong_words = ["production", "benchmark", "architecture", "security", "performance", "scaling", "optimization"]
        strong_count = sum(1 for word in strong_words if word in answer.lower())
        if q_qual == "high" and strong_count > 0:
            score_adjustment += min(0.08, strong_count * 0.03)

        if q_qual == "high":
            if any(char.isdigit() for char in answer):
                score_adjustment += 0.03
            if answer.count(",") >= 2:
                score_adjustment += 0.02

        final_score = clamp01(expected_score + score_adjustment)

        if final_score > 0.8:
            feedback = random.sample(FEEDBACK_EXCELLENT, k=min(3, len(FEEDBACK_EXCELLENT)))
        elif final_score >= 0.65:
            feedback = random.sample(FEEDBACK_GOOD, k=min(3, len(FEEDBACK_GOOD)))
        elif final_score >= 0.45:
            feedback = random.sample(FEEDBACK_AVERAGE, k=min(2, len(FEEDBACK_AVERAGE)))
        else:
            feedback = random.sample(FEEDBACK_POOR, k=min(2, len(FEEDBACK_POOR)))

        scores = {
            "correctness": round(clamp01(final_score + random.uniform(-0.05, 0.05)), 2),
            "depth": round(clamp01(final_score + random.uniform(-0.10, 0.10)), 2),
            "clarity": round(clamp01(final_score + random.uniform(-0.05, 0.05)), 2),
            "practicality": round(clamp01(final_score + random.uniform(-0.10, 0.10)), 2),
            "coverage": round(clamp01(final_score + random.uniform(-0.10, 0.10)), 2),
            "final": round(clamp01(final_score), 2)
        }
        all_scores.append(final_score)

        conversation.append({
            "sequence_number": i,
            "question": question,
            "answer": answer,
            "scores": scores,
            "feedback": feedback
        })

    avg_score = statistics.mean(all_scores)
    overview = map_score_to_overview(avg_score)

    primary_subtopic = main_subtopics[0] if main_subtopics else current_subtopic

    assessment = build_assessment(overview, skill, primary_subtopic, seniority, role, num_questions, avg_score)
    strengths = build_strengths(overview, skill, primary_subtopic, seniority, role)
    weaknesses = build_weaknesses(overview, skill, primary_subtopic, seniority, role)
    recommendations = build_recommendations(overview, skill, primary_subtopic, seniority, role)

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
    print(f"\nTarget: {TARGET_SESSIONS} sessions")
    print(f"Output: {OUTPUT_FILE}\n")

    overview_dist = {k: 0 for k in ["EXCELLENT", "GOOD", "AVERAGE", "BELOW AVERAGE", "POOR"]}
    sample_session = None

    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        for i in range(1, TARGET_SESSIONS + 1):
            s = create_session()
            if sample_session is None:
                sample_session = s

            ov = s["output"]["overview"]
            overview_dist[ov] = overview_dist.get(ov, 0) + 1

            f.write(json.dumps(s, ensure_ascii=False) + '\n')

            if i % 1000 == 0:
                print(f"  Generated {i:,} sessions...")

    print(f"\n[OK] Generated {TARGET_SESSIONS:,} sessions")

    print("\n[Overview Distribution]")
    for ov in ["EXCELLENT", "GOOD", "AVERAGE", "BELOW AVERAGE", "POOR"]:
        count = overview_dist.get(ov, 0)
        pct = (count / TARGET_SESSIONS) * 100 if TARGET_SESSIONS > 0 else 0
        print(f"   {ov}: {count:,} ({pct:.1f}%)")

    print(f"\n[Writing to: {OUTPUT_FILE}]")

    if sample_session is not None:
        sample = sample_session
        print("\n[Sample session]")
        print(f"   Role: {sample['input']['role']}")
        print(f"   Seniority: {sample['input']['seniority']}")
        print(f"   Skills: {', '.join(sample['input']['skills'])}")
        print(f"   Questions: {sample['input']['total_questions']}")
        print(f"   Overview: {sample['output']['overview']}")
        print(f"   Strengths: {len(sample['output']['strengths'])} items")
        print(f"   Weaknesses: {len(sample['output']['weaknesses'])} items")

    print("\n" + "=" * 80)
    print("[OK] DATASET GENERATION COMPLETE")
    print("=" * 80)
    print(f"\nOutput: {OUTPUT_FILE}")
    print(f"Total: {TARGET_SESSIONS:,} sessions")