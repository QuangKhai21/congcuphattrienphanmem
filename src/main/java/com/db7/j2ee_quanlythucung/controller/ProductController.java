package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.Product;
import com.db7.j2ee_quanlythucung.repository.ProductCategoryRepository;
import com.db7.j2ee_quanlythucung.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    /** 商品列表（公开） */
    @GetMapping("/products")
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Pageable pageable = PageRequest.of(page, 12);
        String kw = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
        Long catId = categoryId != null && categoryId > 0 ? categoryId : null;
        Page<Product> productPage =
                productRepository.searchActive(Product.ProductStatus.ACTIVE, catId, kw, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("totalPages", Math.max(1, productPage.getTotalPages()));
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("categories", productCategoryRepository.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        return "products/list";
    }

    /** 商品详情（公开，仅数字 id，避免与 /products/admin 冲突） */
    @GetMapping("/products/{id:\\d+}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        model.addAttribute("product", product);
        return "products/detail";
    }

    /** 后台：新增表单 */
    @GetMapping("/products/admin/new")
    public String adminNewForm(Model model) {
        model.addAttribute("product", Product.builder().stockQuantity(0).status(Product.ProductStatus.ACTIVE).build());
        model.addAttribute("categories", productCategoryRepository.findAll());
        return "products/form";
    }

    /** 后台：创建 */
    @PostMapping("/products/admin")
    public String adminCreate(
            @Valid @ModelAttribute("product") Product product,
            BindingResult result,
            @RequestParam Long categoryId,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (productRepository.existsBySku(product.getSku())) {
            result.rejectValue("sku", "duplicate", "SKU đã tồn tại");
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", productCategoryRepository.findAll());
            return "products/form";
        }
        product.setCategory(productCategoryRepository.findById(categoryId).orElseThrow());
        if (product.getStockQuantity() == null) {
            product.setStockQuantity(0);
        }
        if (product.getStatus() == null) {
            product.setStatus(Product.ProductStatus.ACTIVE);
        }
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("success", "Đã thêm sản phẩm");
        return "redirect:/products";
    }

    /** 后台：编辑表单 */
    @GetMapping("/products/admin/{id}/edit")
    public String adminEditForm(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", product);
        model.addAttribute("categories", productCategoryRepository.findAll());
        return "products/form";
    }

    /** 后台：更新 */
    @PostMapping("/products/admin/{id}")
    public String adminUpdate(
            @PathVariable Long id,
            @Valid @ModelAttribute("product") Product product,
            BindingResult result,
            @RequestParam Long categoryId,
            Model model,
            RedirectAttributes redirectAttributes) {
        Product existing = productRepository.findById(id).orElseThrow();
        if (result.hasErrors()) {
            product.setId(id);
            product.setSku(existing.getSku());
            model.addAttribute("categories", productCategoryRepository.findAll());
            return "products/form";
        }
        existing.setName(product.getName());
        existing.setCategory(productCategoryRepository.findById(categoryId).orElseThrow());
        existing.setPrice(product.getPrice());
        existing.setSalePrice(product.getSalePrice());
        existing.setStockQuantity(product.getStockQuantity() != null ? product.getStockQuantity() : 0);
        existing.setUnit(product.getUnit());
        existing.setStatus(product.getStatus() != null ? product.getStatus() : Product.ProductStatus.ACTIVE);
        existing.setDescription(product.getDescription());
        existing.setImageUrl(product.getImageUrl());
        productRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật sản phẩm");
        return "redirect:/products/" + id;
    }
}
