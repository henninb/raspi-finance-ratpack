package finance.service

import finance.domain.Category
import finance.repositories.CategoryRepository
import finance.services.CategoryService
import spock.lang.Specification

class CategoryServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    CategoryService service = new CategoryService(categoryRepository)

    def 'categories delegates to repository'() {
        given:
        Category c = new Category(categoryName: 'food')
        categoryRepository.categories() >> [c]

        when:
        List<Category> result = service.categories()

        then:
        result == [c]
    }

    def 'categoryInsert sets timestamps and inserts new category'() {
        given:
        Category category = new Category(categoryName: 'food', owner: 'henninb@gmail.com', activeStatus: true)
        categoryRepository.category('food') >> null

        when:
        Category result = service.categoryInsert(category)

        then:
        1 * categoryRepository.categoryInsert(category)
        result == category
        result.dateAdded != null
        result.dateUpdated != null
    }

    def 'categoryInsert returns existing category without re-inserting'() {
        given:
        Category existing = new Category(categoryName: 'food', categoryId: 1L)
        categoryRepository.category('food') >> existing

        when:
        Category result = service.categoryInsert(new Category(categoryName: 'food'))

        then:
        0 * categoryRepository.categoryInsert(_)
        result == existing
    }

    def 'categoryUpdate throws when category not found'() {
        given:
        categoryRepository.category('missing') >> null

        when:
        service.categoryUpdate(new Category(categoryName: 'missing'))

        then:
        thrown(RuntimeException)
    }

    def 'categoryUpdate delegates and returns refreshed category'() {
        given:
        Category existing = new Category(categoryName: 'food')
        Category updated = new Category(categoryName: 'food', activeStatus: false)
        categoryRepository.category('food') >>> [existing, updated]

        when:
        Category result = service.categoryUpdate(new Category(categoryName: 'food', activeStatus: false))

        then:
        1 * categoryRepository.categoryUpdate(_)
        result.activeStatus == false
    }

    def 'categoryDelete returns false when category not found'() {
        given:
        categoryRepository.category('missing') >> null

        expect:
        !service.categoryDelete('missing')
    }

    def 'categoryDelete returns true when category exists'() {
        given:
        categoryRepository.category('food') >> new Category(categoryName: 'food')
        categoryRepository.categoryDelete('food') >> true

        expect:
        service.categoryDelete('food')
    }

    def 'categoryMerge delegates merge and returns target category'() {
        given:
        Category target = new Category(categoryName: 'groceries')
        categoryRepository.category('groceries') >> target

        when:
        Category result = service.categoryMerge('food', 'groceries')

        then:
        1 * categoryRepository.categoryMerge('food', 'groceries')
        result == target
    }
}
