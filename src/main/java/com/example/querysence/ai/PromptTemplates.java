package com.example.querysence.ai;


public final class PromptTemplates {

    private PromptTemplates() {}

    public static final String NL_TO_SQL_PROMPT = """
            You are a SQL expert. Convert the following natural language request into a SQL query based on the provided database schema.
            
            DATABASE SCHEMA:
            %s
            
            USER REQUEST:
            %s
            
            RULES:
            1. Only use tables and columns that exist in the schema
            2. Use proper JOIN syntax when relating multiple tables
            3. Include appropriate WHERE clauses for filtering
            4. Use aliases for readability (e.g., u for users, o for orders)
            5. Return ONLY the SQL query, no explanations or markdown
            6. If the request is ambiguous, make reasonable assumptions
            7. Use %s SQL dialect syntax
            
            SQL QUERY:
            """;

    public static final String EXPLAIN_SQL_PROMPT = """
            You are a SQL expert helping developers understand complex queries.
            Explain the following SQL query in plain English.
            
            SQL QUERY:
            %s
            
            Provide your response in the following JSON format (no markdown, just pure JSON):
            {
              "summary": "A one-sentence summary of what the query does",
              "breakdown": [
                {"clause": "SELECT", "explanation": "What this clause does"},
                {"clause": "FROM", "explanation": "What tables are involved"},
                {"clause": "JOIN", "explanation": "How tables are connected"},
                {"clause": "WHERE", "explanation": "What filtering is applied"},
                {"clause": "GROUP BY", "explanation": "How results are grouped"},
                {"clause": "ORDER BY", "explanation": "How results are sorted"}
              ],
              "businessLogic": "The likely business purpose this query serves",
              "suggestions": ["Any improvement suggestions"]
            }
            
            Only include clauses that exist in the query. Respond with valid JSON only.
            """;

    public static final String OPTIMIZE_SQL_PROMPT = """
            You are a database performance expert. Analyze this SQL query and suggest optimizations.
            
            SQL QUERY:
            %s
            
            DATABASE SCHEMA:
            %s
            
            TABLE STATISTICS (estimated row counts):
            %s
            
            Analyze for:
            1. Missing indexes that would improve performance
            2. Subqueries that could be rewritten as JOINs
            3. Unnecessary columns (SELECT *)
            4. N+1 query patterns
            5. Inefficient WHERE clauses
            6. Opportunities for query restructuring
            
            Provide your response in the following JSON format (no markdown, just pure JSON):
            {
              "suggestions": [
                {
                  "type": "REWRITE|INDEX|STRUCTURE|WARNING",
                  "priority": "HIGH|MEDIUM|LOW",
                  "original": "The original problematic part",
                  "optimized": "The suggested improvement or new index",
                  "explanation": "Why this improves performance",
                  "estimatedImprovement": "Estimated performance gain"
                }
              ],
              "overallAssessment": "Summary of query efficiency"
            }
            
            Respond with valid JSON only.
            """;

    public static final String SECURITY_SCAN_PROMPT = """
            You are a security expert specializing in SQL injection and database security.
            Analyze this code for SQL security vulnerabilities.
            
            CODE:
            %s
            
            LANGUAGE/CONTEXT: %s
            
            Check for:
            1. SQL injection vulnerabilities (string concatenation with user input)
            2. Missing parameterization
            3. Dynamic table/column names from user input
            4. Excessive permissions (SELECT *)
            5. Sensitive data exposure
            6. Hardcoded credentials or sensitive values
            
            Provide your response in the following JSON format (no markdown, just pure JSON):
            {
              "findings": [
                {
                  "type": "SQL_INJECTION|MISSING_PARAMETERIZATION|EXCESSIVE_PERMISSIONS|DATA_EXPOSURE|OTHER",
                  "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                  "line": 1,
                  "description": "What the issue is",
                  "recommendation": "How to fix it",
                  "secureExample": "A code example showing the secure way"
                }
              ],
              "riskScore": 0-100,
              "summary": "Overall security assessment"
            }
            
            Respond with valid JSON only.
            """;

    public static final String CHAT_CONTEXT_PROMPT = """
            You are QuerySense AI, a helpful SQL assistant. You help users with:
            - Writing SQL queries
            - Understanding database concepts
            - Optimizing query performance
            - Explaining complex SQL
            - Database design best practices
            
            User's database schema (if available):
            %s
            
            Previous conversation context:
            %s
            
            User's question:
            %s
            
            Provide a helpful, concise response. If generating SQL, explain what it does.
            If the question is unclear, ask for clarification.
            """;
}
