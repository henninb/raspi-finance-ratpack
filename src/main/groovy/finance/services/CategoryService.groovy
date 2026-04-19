package finance.services

import finance.domain.Category
import finance.repositories.CategoryRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Log
@CompileStatic
class CategoryService implements Service {

    private CategoryRepository categoryRepository

    @Inject
    CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository
    }

    List<Category> categories() {
        return categoryRepository.categories()
    }

    Category category(String categoryName) {
        return categoryRepository.category(categoryName)
    }

    Category categoryInsert(Category category) {
        category.dateUpdated = new Timestamp(System.currentTimeMillis())
        category.dateAdded = new Timestamp(System.currentTimeMillis())
        if (categoryRepository.category(category.categoryName)) {
            return categoryRepository.category(category.categoryName)
        }
        categoryRepository.categoryInsert(category)
        log.info("inserted category ${category.categoryName}")
        return category
    }

    Category categoryUpdate(Category category) {
        Category existing = categoryRepository.category(category.categoryName)
        if (!existing) {
            throw new RuntimeException("category not found: ${category.categoryName}")
        }
        categoryRepository.categoryUpdate(category)
        return categoryRepository.category(category.categoryName)
    }

    boolean categoryDelete(String categoryName) {
        Category existing = categoryRepository.category(categoryName)
        if (!existing) {
            return false
        }
        return categoryRepository.categoryDelete(categoryName)
    }

    Category categoryMerge(String oldCategoryName, String newCategoryName) {
        categoryRepository.categoryMerge(oldCategoryName, newCategoryName)
        log.info("merged category ${oldCategoryName} into ${newCategoryName}")
        return categoryRepository.category(newCategoryName)
    }
}
