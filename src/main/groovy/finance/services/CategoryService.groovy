package finance.services

import finance.domain.Category
import finance.repositories.CategoryRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service
import javax.inject.Inject

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

//    Category insertCategory( Category category ) {
//        this.categoryRepository.insertCategory(category)
//    }
}
