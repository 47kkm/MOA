package moa.store.internal;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import moa.store.StoreInfo;
import moa.store.StoreService;
import moa.store.StoreType;

// JSON ↔ 레코드 변환만 한다. 규칙 검사는 Store와 StoreService에 있다 (04 설계 0.5)
@RestController
@RequestMapping("/api/stores")
class StoreController {

    private final StoreService storeService;

    StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StoreInfo register(@RequestBody StoreRequest request) {
        return storeService.register(request.name(), request.type());
    }

    @GetMapping
    List<StoreInfo> findAll() {
        return storeService.findAll();
    }

    @GetMapping("/{id}")
    StoreInfo get(@PathVariable Long id) {
        return storeService.get(id);
    }

    record StoreRequest(String name, StoreType type) {
    }
}
