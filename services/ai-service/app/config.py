import os

# Environment: standardise on SPRING_PROFILES_ACTIVE or fallback to ENV
ENV = os.getenv('SPRING_PROFILES_ACTIVE', os.getenv('ENV', 'dev'))
PORT = int(os.getenv('AI_SERVICE_PORT', '8000'))
DATABASE_URL = os.getenv('DATABASE_URL', '')
JWT_SECRET = os.getenv('JWT_SECRET', '')