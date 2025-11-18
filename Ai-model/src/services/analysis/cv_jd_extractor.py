import re
import logging
from typing import Dict, List, Set, Optional
from dataclasses import dataclass, field

logger = logging.getLogger(__name__)


# Experience thresholds for level inference
YEARS_JUNIOR = 2
YEARS_MID = 5
YEARS_SENIOR = 8


@dataclass
class CVExtraction:
    """Thông tin extracted từ CV"""
    years_experience: Optional[float] = None
    technologies: List[str] = field(default_factory=list)
    projects: List[str] = field(default_factory=list)
    achievements: List[str] = field(default_factory=list)
    inferred_level: Optional[str] = None


@dataclass
class JDExtraction:
    """Thông tin extracted từ Job Description"""
    must_have_skills: List[str] = field(default_factory=list)
    nice_to_have_skills: List[str] = field(default_factory=list)
    responsibilities: List[str] = field(default_factory=list)
    required_years: Optional[float] = None
    inferred_level: Optional[str] = None


@dataclass
class SkillGapAnalysis:
    """Phân tích khoảng cách kỹ năng giữa CV và JD"""
    has_skills: List[str] = field(default_factory=list)
    missing_must_have: List[str] = field(default_factory=list)
    missing_nice_to_have: List[str] = field(default_factory=list)
    focus_areas: List[str] = field(default_factory=list)


class CVJDExtractor:
    """Extract thông tin thông minh từ CV và JD"""
    
    # Comprehensive tech keywords (200+ technologies)
    TECH_KEYWORDS = {
        # Programming Languages
        'java', 'python', 'javascript', 'typescript', 'c++', 'c#', 'go', 'golang', 'rust', 'kotlin',
        'scala', 'ruby', 'php', 'swift', 'objective-c', 'r', 'matlab', 'perl', 'bash', 'shell',
        
        # Java Ecosystem
        'spring', 'spring boot', 'spring mvc', 'spring security', 'spring cloud', 'hibernate',
        'jpa', 'jdbc', 'maven', 'gradle', 'junit', 'mockito', 'testng', 'jsp', 'servlet',
        'tomcat', 'jetty', 'wildfly', 'jboss', 'websphere', 'weblogic',
        
        # JavaScript/Frontend
        'react', 'react.js', 'reactjs', 'angular', 'vue', 'vue.js', 'vuejs', 'svelte', 'next.js', 'nuxt',
        'jquery', 'backbone', 'ember', 'webpack', 'vite', 'rollup', 'babel', 'eslint',
        'redux', 'mobx', 'rxjs', 'sass', 'scss', 'less', 'tailwind', 'bootstrap', 'material-ui',
        
        # Backend Frameworks
        'node.js', 'express', 'express.js', 'nest.js', 'fastify', 'koa', 'django', 'flask',
        'fastapi', 'rails', 'laravel', 'symfony', 'asp.net', '.net core', 'gin', 'echo',
        
        # Databases
        'mysql', 'postgresql', 'postgres', 'oracle', 'sql server', 'mongodb', 'cassandra',
        'redis', 'memcached', 'elasticsearch', 'dynamodb', 'couchdb', 'neo4j', 'influxdb',
        'mariadb', 'sqlite', 'h2', 'firestore', 'cosmos db',
        
        # Cloud & DevOps
        'aws', 'azure', 'gcp', 'google cloud', 'ec2', 's3', 'lambda', 'cloudformation',
        'terraform', 'ansible', 'puppet', 'chef', 'docker', 'kubernetes', 'k8s', 'openshift',
        'helm', 'jenkins', 'gitlab ci', 'github actions', 'circleci', 'travis ci', 'bamboo',
        'argocd', 'spinnaker', 'nexus', 'artifactory',
        
        # Message Queues & Streaming
        'kafka', 'rabbitmq', 'activemq', 'redis pub/sub', 'aws sqs', 'aws sns', 'kinesis',
        'pulsar', 'nats', 'zeromq',
        
        # Monitoring & Logging
        'prometheus', 'grafana', 'datadog', 'new relic', 'splunk', 'elk', 'elasticsearch',
        'logstash', 'kibana', 'fluentd', 'sentry', 'jaeger', 'zipkin',
        
        # API & Communication
        'rest', 'restful', 'graphql', 'grpc', 'soap', 'websocket', 'http/2', 'http/3',
        'oauth', 'jwt', 'saml', 'openid',
        
        # Testing
        'selenium', 'cypress', 'jest', 'mocha', 'chai', 'jasmine', 'pytest', 'unittest',
        'postman', 'jmeter', 'gatling', 'locust', 'cucumber', 'testcontainers',
        
        # Architecture & Patterns
        'microservices', 'monolith', 'serverless', 'event-driven', 'cqrs', 'event sourcing',
        'clean architecture', 'hexagonal', 'domain-driven design', 'ddd', 'solid', 'design patterns',
        
        # Big Data & ML
        'hadoop', 'spark', 'flink', 'airflow', 'databricks', 'snowflake', 'redshift',
        'tensorflow', 'pytorch', 'scikit-learn', 'pandas', 'numpy', 'jupyter',
        
        # Mobile
        'android', 'ios', 'react native', 'flutter', 'xamarin', 'ionic',
        
        # Version Control & Collaboration
        'git', 'github', 'gitlab', 'bitbucket', 'svn', 'mercurial', 'jira', 'confluence',
        
        # Security
        'owasp', 'penetration testing', 'vulnerability', 'encryption', 'ssl/tls', 'pki',
        'oauth2', 'iam', 'rbac', 'firewall', 'waf',
    }
    
    # Experience patterns
    EXPERIENCE_PATTERNS = [
        r'(\d+(?:\.\d+)?)\+?\s*(?:years?|yrs?)\s*(?:of\s*)?(?:experience|exp)',
        r'(?:experience|exp)\s*(?:of\s*)?(\d+(?:\.\d+)?)\+?\s*(?:years?|yrs?)',
        r'(\d+(?:\.\d+)?)\+?\s*(?:years?|yrs?)',
        r'over\s*(\d+)\s*(?:years?|yrs?)',
        r'more than\s*(\d+)\s*(?:years?|yrs?)',
    ]
    
    # Level indicators
    LEVEL_INDICATORS = {
        'junior': ['junior', 'entry', 'entry-level', 'fresh', 'graduate', 'intern', '0-2 years', '1 year', '2 years'],
        'mid': ['mid-level', 'mid', 'intermediate', '2-5 years', '3 years', '4 years', '5 years', 'developer ii'],
        'senior': ['senior', 'lead', 'principal', 'staff', 'architect', '5+ years', '6+ years', '7+ years', 'expert', 'advanced'],
    }
    
    def extract_cv(self, cv_text: str) -> CVExtraction:
        """Extract thông tin từ CV"""
        if not cv_text or not cv_text.strip():
            return CVExtraction()
        
        cv_lower = cv_text.lower()
        
        extraction = CVExtraction()
        
        # Extract years of experience
        extraction.years_experience = self._extract_years_experience(cv_lower)
        
        # Extract technologies
        extraction.technologies = self._extract_technologies(cv_lower)
        
        # Extract projects (đơn giản: tìm keywords)
        extraction.projects = self._extract_projects(cv_text)
        
        # Extract achievements (bullet points, metrics)
        extraction.achievements = self._extract_achievements(cv_text)
        
        # Infer level
        extraction.inferred_level = self._infer_level(cv_lower, extraction.years_experience)
        
        logger.info(f"CV Extraction: {extraction.years_experience} years, {len(extraction.technologies)} techs, level={extraction.inferred_level}")
        
        return extraction
    
    def extract_jd(self, jd_text: str) -> JDExtraction:
        """Extract thông tin từ Job Description"""
        if not jd_text or not jd_text.strip():
            return JDExtraction()
        
        jd_lower = jd_text.lower()
        
        extraction = JDExtraction()
        
        # Extract required years
        extraction.required_years = self._extract_years_experience(jd_lower)
        
        # Extract must-have vs nice-to-have skills
        must_have, nice_to_have = self._extract_jd_skills(jd_text, jd_lower)
        extraction.must_have_skills = must_have
        extraction.nice_to_have_skills = nice_to_have
        
        # Extract responsibilities
        extraction.responsibilities = self._extract_responsibilities(jd_text)
        
        # Infer level
        extraction.inferred_level = self._infer_level(jd_lower, extraction.required_years)
        
        logger.info(f"JD Extraction: {extraction.required_years} years required, {len(extraction.must_have_skills)} must-have, {len(extraction.nice_to_have_skills)} nice-to-have")
        
        return extraction
    
    def analyze_skill_gap(self, cv_extraction: CVExtraction, jd_extraction: JDExtraction) -> SkillGapAnalysis:
        """Phân tích skill gaps giữa CV và JD"""
        cv_skills_lower = set(s.lower() for s in cv_extraction.technologies)
        must_have_lower = set(s.lower() for s in jd_extraction.must_have_skills)
        nice_to_have_lower = set(s.lower() for s in jd_extraction.nice_to_have_skills)
        
        analysis = SkillGapAnalysis()
        
        # Skills candidate has
        analysis.has_skills = list(cv_skills_lower & (must_have_lower | nice_to_have_lower))
        
        # Missing must-have (CRITICAL)
        analysis.missing_must_have = list(must_have_lower - cv_skills_lower)
        
        # Missing nice-to-have (GOOD TO PROBE)
        analysis.missing_nice_to_have = list(nice_to_have_lower - cv_skills_lower)
        
        # Focus areas: Prioritize must-have gaps, then nice-to-have
        analysis.focus_areas = analysis.missing_must_have[:3] + analysis.missing_nice_to_have[:2]
        
        logger.info(f"Skill Gap: {len(analysis.missing_must_have)} critical gaps, {len(analysis.missing_nice_to_have)} nice-to-have gaps")
        logger.info(f"Focus areas: {', '.join(analysis.focus_areas[:5])}")
        
        return analysis
    
    def _extract_years_experience(self, text_lower: str) -> Optional[float]:
        """Extract years of experience từ text"""
        for pattern in self.EXPERIENCE_PATTERNS:
            match = re.search(pattern, text_lower, re.IGNORECASE)
            if match:
                try:
                    years = float(match.group(1))
                    return years
                except (ValueError, IndexError):
                    continue
        return None
    
    def _extract_technologies(self, text_lower: str) -> List[str]:
        """Extract technologies từ text"""
        found_techs = set()
        
        # Sort by length (longest first) để match "spring boot" trước "spring"
        sorted_keywords = sorted(self.TECH_KEYWORDS, key=len, reverse=True)
        
        for tech in sorted_keywords:
            # Use word boundary để tránh false positive
            pattern = r'\b' + re.escape(tech) + r'\b'
            if re.search(pattern, text_lower):
                found_techs.add(tech)
        
        return sorted(list(found_techs))
    
    def _extract_projects(self, text: str) -> List[str]:
        """Extract project mentions từ CV"""
        projects = []
        
        # Common project keywords
        project_keywords = [
            r'built\s+([^.]+?)(?:using|with|for)',
            r'developed\s+([^.]+?)(?:using|with|for)',
            r'created\s+([^.]+?)(?:using|with|for)',
            r'implemented\s+([^.]+?)(?:using|with|for)',
            r'designed\s+(?:and\s+)?(?:developed\s+)?([^.]+?)(?:using|with|for)',
            r'project:\s*([^.\n]+)',
        ]
        
        for pattern in project_keywords:
            matches = re.findall(pattern, text, re.IGNORECASE)
            projects.extend([m.strip() for m in matches if len(m.strip()) > 10])
        
        return projects[:5]  # Top 5 projects
    
    def _extract_achievements(self, text: str) -> List[str]:
        """Extract achievements (metrics, improvements)"""
        achievements = []
        
        # Patterns for achievements with metrics
        achievement_patterns = [
            r'(?:increased|improved|reduced|decreased|optimized)\s+[^.]+?\d+%',
            r'\d+%\s+(?:increase|improvement|reduction|decrease|optimization)',
            r'(?:from\s+\d+[^.]+?to\s+\d+)',
        ]
        
        for pattern in achievement_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            achievements.extend([m.strip() for m in matches])
        
        return achievements[:5]  # Top 5 achievements
    
    def _extract_jd_skills(self, jd_text: str, jd_lower: str) -> tuple[List[str], List[str]]:
        """Extract must-have vs nice-to-have skills từ JD"""
        must_have = set()
        nice_to_have = set()
        
        # Improved: Split by more specific patterns
        # Must-have patterns
        must_have_pattern = r'(?:required|must\s+have|requirements?|qualifications?|essential|mandatory)[\s:]*([^§]+?)(?:nice\s+to\s+have|preferred|bonus|responsibilities|what\s+we\s+offer|$)'
        must_match = re.search(must_have_pattern, jd_text, re.IGNORECASE | re.DOTALL)
        
        # Nice-to-have patterns
        nice_pattern = r'(?:nice\s+to\s+have|preferred|bonus|plus|desirable|optional)[\s:]*([^§]+?)(?:responsibilities|what\s+we\s+offer|requirements|$)'
        nice_match = re.search(nice_pattern, jd_text, re.IGNORECASE | re.DOTALL)
        
        # Extract techs from must-have section
        if must_match:
            must_have_text = must_match.group(1).lower()[:800]  # Increase to 800 chars
            must_have = set(self._extract_technologies(must_have_text))
            logger.debug(f"Found must-have section: {must_have_text[:100]}...")
        
        # Extract techs from nice-to-have section
        if nice_match:
            nice_have_text = nice_match.group(1).lower()[:800]
            nice_to_have = set(self._extract_technologies(nice_have_text))
            logger.debug(f"Found nice-to-have section: {nice_have_text[:100]}...")
        
        # If still empty, try alternative approach
        if not must_have and not nice_to_have:
            logger.warning("Could not find clear must-have/nice-to-have sections, using fallback")
            all_techs = self._extract_technologies(jd_lower)
            
            # Check for explicit "must" or "required" mentions
            for tech in all_techs:
                tech_context_pattern = rf'\b{re.escape(tech)}\b.{{0,30}}(?:required|must|essential)'
                if re.search(tech_context_pattern, jd_lower, re.IGNORECASE):
                    must_have.add(tech)
                else:
                    # Check reverse: "required... tech"
                    reverse_pattern = rf'(?:required|must|essential).{{0,50}}\b{re.escape(tech)}\b'
                    if re.search(reverse_pattern, jd_lower, re.IGNORECASE):
                        must_have.add(tech)
            
            # Remaining techs are nice-to-have
            nice_to_have = set(all_techs) - must_have
        
        # Remove overlap (must-have takes priority)
        nice_to_have = nice_to_have - must_have
        
        return sorted(list(must_have)), sorted(list(nice_to_have))
    
    def _extract_responsibilities(self, jd_text: str) -> List[str]:
        """Extract job responsibilities"""
        responsibilities = []
        
        # Find responsibilities section
        resp_patterns = [
            r'(?:responsibilities|duties|you will)[\s:]+([^§]+?)(?:requirements|qualifications|skills|$)',
        ]
        
        for pattern in resp_patterns:
            match = re.search(pattern, jd_text, re.IGNORECASE | re.DOTALL)
            if match:
                resp_text = match.group(1)
                # Split by bullet points or newlines
                items = re.split(r'[\n•\-\*]', resp_text)
                responsibilities = [item.strip() for item in items if len(item.strip()) > 20][:5]
                break
        
        return responsibilities
    
    def _infer_level(self, text_lower: str, years: Optional[float]) -> str:
        """Infer level từ text và years of experience"""
        # Check explicit level mentions
        for level, indicators in self.LEVEL_INDICATORS.items():
            for indicator in indicators:
                if indicator in text_lower:
                    return level.capitalize()
        
        # Infer từ years
        if years is not None:
            if years <= YEARS_JUNIOR:
                return 'Junior'
            elif years <= YEARS_MID:
                return 'Mid-level'
            else:
                return 'Senior'
        
        return 'Mid-level'  # Default


# Singleton instance
cv_jd_extractor = CVJDExtractor()
