package com.fpolizzi.smartlead.controller.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Set;

/**
 * Keeps user-supplied sort parameters on the web list pages to a whitelist of columns.
 * An unknown property would otherwise make Spring Data throw PropertyReferenceException.
 */
final class SortSupport {

    /** Used when the request asks for no valid column. Matches the default of the list pages. */
    static final Sort FALLBACK = Sort.by(Sort.Direction.DESC, "createdAt");

    private SortSupport() {
    }

    /** Drops sort orders for properties outside the whitelist, and falls back to createdAt DESC if none remain. */
    static Pageable sanitize(Pageable pageable, Set<String> sortableProperties) {
        List<Sort.Order> valid = pageable.getSort().stream()
                .filter(order -> sortableProperties.contains(order.getProperty()))
                .toList();
        Sort sort = valid.isEmpty() ? FALLBACK : Sort.by(valid);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    /** Exposes the active column and direction to the template, which shows the arrow and builds links. */
    static void addSortAttributes(Model model, Sort sort) {
        Sort.Order first = sort.iterator().next();
        model.addAttribute("sortProperty", first.getProperty());
        model.addAttribute("sortDirection", first.isAscending() ? "asc" : "desc");
    }
}
