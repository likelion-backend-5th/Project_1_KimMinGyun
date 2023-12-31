package com.example.mutsamarket.service;

import com.example.mutsamarket.dto.salesItemDto.SalesItemEnrollDto;
import com.example.mutsamarket.dto.salesItemDto.SalesItemReadDto;
import com.example.mutsamarket.entity.SalesItem;
import com.example.mutsamarket.entity.UserEntity;
import com.example.mutsamarket.repository.SalesItemRepository;
import com.example.mutsamarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesItemService {
    private final SalesItemRepository salesItemRepository;
    private final UserRepository userRepository;

    // 물픔 등록
    public void enrollSalesItem(SalesItemEnrollDto dto, Authentication authentication) {

        String loginUser = authentication.getName();
        Optional<UserEntity> optionalUser = userRepository.findByUsername(loginUser);
        UserEntity user = optionalUser.get();

        SalesItem newItem = new SalesItem();
        newItem.setTitle(dto.getTitle());
        newItem.setDescription(dto.getDescription());
        newItem.setMinPriceWanted(dto.getMinPriceWanted());
        newItem.setStatus("판매중");
        newItem.setUser(user);
        newItem = salesItemRepository.save(newItem);

        // salesItemRepository.findAll().forEach(System.out::println); //결과 보기위해서
        // userRepository.findAll().forEach(System.out::println);

    }

    //물품 단일 조회, 인증 x
    public SalesItemReadDto readItem(Long itemId) {
        Optional<SalesItem> optionalSalesItem
                = salesItemRepository.findById(Math.toIntExact(itemId));

        if (optionalSalesItem.isPresent())
            // DTO로 전환 후 반환
            return SalesItemReadDto.fromEntity(optionalSalesItem.get());

            // 아니면 404
        else throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    //물품 전체 조회, 인증 x
    public List<SalesItemReadDto> readItemAll() {

        List<SalesItem> salesItemList = salesItemRepository.findAll();
        if (salesItemList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<SalesItemReadDto> salesItemReadDtoList = new ArrayList<>();
        for (SalesItem salesItem : salesItemList) {
            SalesItemReadDto salesItemDto = SalesItemReadDto.fromEntity(salesItem);
            salesItemReadDtoList.add(salesItemDto);
        }
        return salesItemReadDtoList;

    }

    //물품 페이지 조회, 인증 x
    public Page<SalesItemReadDto> readPageItem(Long page, Long limit) {

        Pageable pageable = PageRequest.of(Math.toIntExact(page), Math.toIntExact(limit));
        Page<SalesItem> salesItemPage
                = salesItemRepository.findAll(pageable);

        Page<SalesItemReadDto> salesItemDtoPage = salesItemPage.map(SalesItemReadDto::fromEntity);

        return salesItemDtoPage;

    }

    //물품 판매글 수정
    public void updateSalesItem(
            Long itemId,
            SalesItemEnrollDto dto,
            Authentication authentication
    ) {
        String username = authentication.getName();

        Optional<SalesItem> optionalSalesItem = salesItemRepository.findByItemIdAndUsername(itemId, username);

        if (optionalSalesItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        else {
            SalesItem updateItem = optionalSalesItem.get();
            updateItem.setTitle(dto.getTitle());
            updateItem.setDescription(dto.getDescription());
            updateItem.setMinPriceWanted(dto.getMinPriceWanted());
            salesItemRepository.save(updateItem);

        }
    }

    //물푼 판매글에 이미지 첨부
    public SalesItemEnrollDto updateMarketImage(MultipartFile Image, Long id, Authentication authentication) {

        String username = authentication.getName();

        Optional<SalesItem> optionalMarket = salesItemRepository.findByItemIdAndUsername(id, username);
        if (optionalMarket.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        else {
            SalesItem salesItem = optionalMarket.get();
            // 2-1. 폴더만 만드는 과정
            String profileDir = String.format("media/%d/", id);
            log.info(profileDir);
            try {
                Files.createDirectories(Path.of(profileDir));
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            // 2-2. 확장자를 포함한 이미지 이름 만들기
            String originalFilename = Image.getOriginalFilename();
            String[] fileNameSplit = originalFilename.split("\\.");
            String extension = fileNameSplit[fileNameSplit.length - 1];
            String profileFilename = "items." + extension;
            log.info(profileFilename);

            // 2-3. 폴더와 파일 경로를 포함한 이름 만들기
            String profilePath = profileDir + profileFilename;
            log.info(profilePath);

            // 3. MultipartFile 을 저장하기
            try {
                Image.transferTo(Path.of(profilePath));
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            // 4. UserEntity 업데이트 (정적 프로필 이미지를 회수할 수 있는 URL)
            log.info(String.format("/static/%d/%s", id, profileFilename));

            salesItem.setImageUrl(String.format("/static/%d/%s", id, profileFilename));
            return SalesItemEnrollDto.fromEntity(salesItemRepository.save(salesItem));

        }
    }

    // 물품 판매글 삭제
    public boolean deleteItem(Long id, Authentication authentication) {

        String username = authentication.getName();

        Optional<SalesItem> optionalSalesItem = salesItemRepository.findByItemIdAndUsername(id, username);

        if (optionalSalesItem.isPresent()) {
            salesItemRepository.delete(optionalSalesItem.get());
            return true;
        } else return false;

    }

}