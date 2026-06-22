#!/bin/bash
# Fix relative imports to use @/ alias
find src/components -name "*.tsx" -type f -exec sed -i '' 's|from "\.\./\.\./|from "@/|g' {} \;
find src/components -name "*.tsx" -type f -exec sed -i '' 's|from "\.\./|from "@/components/|g' {} \;
find src/pages -name "*.tsx" -type f -exec sed -i '' 's|from "\.\./\.\./|from "@/|g' {} \;
