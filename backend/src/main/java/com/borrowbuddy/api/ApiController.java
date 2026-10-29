package com.borrowbuddy.api;

import com.borrowbuddy.domain.*;
import com.borrowbuddy.repo.*;
import com.borrowbuddy.service.LoanService;
import com.borrowbuddy.security.CampusPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Set<String> CATEGORIES=Set.of("Books","Electronics","Study","Sports","Arts","Music");
    private final ListingRepository listings; private final LoanRequestRepository loans; private final LoanService loanService;
    private final CampusUserRepository users; private final ReviewRepository reviews; private final SavedListingRepository saved;
    private final NotificationRepository notifications;
    public ApiController(ListingRepository listings,LoanRequestRepository loans,LoanService loanService,CampusUserRepository users,ReviewRepository reviews,SavedListingRepository saved,NotificationRepository notifications){this.listings=listings;this.loans=loans;this.loanService=loanService;this.users=users;this.reviews=reviews;this.saved=saved;this.notifications=notifications;}
    public record ListingInput(@NotBlank @Size(max=180) String title,@NotBlank @Size(max=2000) String description,@NotBlank @Size(max=80) String category,@Size(max=2048) @Pattern(regexp="https://.+",message="Image URL must use HTTPS") String imageUrl){}
    public record RequestInput(@NotNull java.time.LocalDate startDate,@NotNull java.time.LocalDate returnDate){}
    public record DecisionInput(@NotNull Boolean approve){}
    public record TransitionInput(@NotNull LoanRequest.Status status){}
    public record PageResponse<T>(List<T> content,int page,int size,long totalElements,int totalPages){}
    @GetMapping("/health") public Map<String,String> health(){return Map.of("status","ok","app","BorrowBuddy");}

    @GetMapping("/listings") @Transactional(readOnly=true)
    public PageResponse<ApiModels.ListingResponse> search(@AuthenticationPrincipal CampusPrincipal user,@RequestParam(required=false)String q,@RequestParam(required=false)String category,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="24")int size){
        if(category!=null&&!category.isBlank()&&!CATEGORIES.contains(category))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown category");
        int safePage=Math.max(0,page),safeSize=Math.min(50,Math.max(1,size));Page<Listing> result=listings.findAll(ListingSpecs.search(q,category,user.campus()),PageRequest.of(safePage,safeSize,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
        return new PageResponse<>(result.map(this::listingResponse).getContent(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());
    }
    @GetMapping("/listings/{id}") @Transactional(readOnly=true)
    public ApiModels.ListingResponse getListing(@PathVariable Long id,@AuthenticationPrincipal CampusPrincipal user){Listing l=listingInCampus(id,user.campus());if(l.status==Listing.Status.DELETED)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Listing not found");return listingResponse(l);}
    @PostMapping("/listings") @ResponseStatus(HttpStatus.CREATED)
    public ApiModels.ListingResponse create(@AuthenticationPrincipal CampusPrincipal user,@Valid @RequestBody ListingInput in){validateCategory(in.category());CampusUser owner=findUser(user.id());Listing l=listings.save(new Listing(user.id(),in.title().trim(),in.description().trim(),in.category(),in.imageUrl(),owner.campus));return listingResponse(l);}
    @PutMapping("/listings/{id}")
    public ApiModels.ListingResponse update(@PathVariable Long id,@AuthenticationPrincipal CampusPrincipal user,@Valid @RequestBody ListingInput in){validateCategory(in.category());Listing l=listingInCampus(id,user.campus());if(!l.ownerId.equals(user.id()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the owner can edit");if(l.status==Listing.Status.DELETED)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Listing not found");l.title=in.title().trim();l.description=in.description().trim();l.category=in.category();l.imageUrl=in.imageUrl();return listingResponse(listings.save(l));}
    @DeleteMapping("/listings/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,@AuthenticationPrincipal CampusPrincipal user){Listing l=listingInCampus(id,user.campus());if(!l.ownerId.equals(user.id()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the owner can remove");loanService.markDeleted(id,user.id());}
    @GetMapping("/me/listings") @Transactional(readOnly=true)
    public List<ApiModels.ListingResponse> ownListings(@AuthenticationPrincipal CampusPrincipal user){return listings.findByOwnerIdAndStatusNotOrderByCreatedAtDesc(user.id(),Listing.Status.DELETED).stream().map(this::listingResponse).toList();}
    @GetMapping("/me/listings/requests") @Transactional(readOnly=true)
    public List<ApiModels.LoanResponse> incomingRequests(@AuthenticationPrincipal CampusPrincipal user){return loans.findForOwner(user.id()).stream().map(this::loanResponse).toList();}

    @PostMapping("/listings/{id}/requests") @ResponseStatus(HttpStatus.CREATED)
    public ApiModels.LoanResponse request(@PathVariable Long id,@AuthenticationPrincipal CampusPrincipal user,@Valid @RequestBody RequestInput in){LoanRequest r=loanService.request(id,user.id(),in.startDate(),in.returnDate());return loanResponse(r);}
    @GetMapping("/me/requests") @Transactional(readOnly=true)
    public List<ApiModels.LoanResponse> ownRequests(@AuthenticationPrincipal CampusPrincipal user){return loans.findByBorrowerIdOrderByCreatedAtDesc(user.id()).stream().map(this::loanResponse).toList();}
    @PostMapping("/requests/{id}/decision")
    public ApiModels.LoanResponse decide(@PathVariable Long id,@AuthenticationPrincipal CampusPrincipal user,@Valid @RequestBody DecisionInput in){return loanResponse(loanService.decide(id,user.id(),in.approve()));}
    @PostMapping("/requests/{id}/transition")
    public ApiModels.LoanResponse transition(@PathVariable Long id,@AuthenticationPrincipal CampusPrincipal user,@Valid @RequestBody TransitionInput in){return loanResponse(loanService.transition(id,user.id(),in.status()));}
    @PostMapping("/requests/{id}/cancel") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id,@AuthenticationPrincipal CampusPrincipal user){loanService.cancel(id,user.id());}

    @GetMapping("/me/saved") @Transactional(readOnly=true)
    public List<ApiModels.ListingResponse> savedListings(@AuthenticationPrincipal CampusPrincipal user){return saved.findByUserId(user.id()).stream().map(s->listings.findById(s.listingId).orElse(null)).filter(Objects::nonNull).filter(l->l.status==Listing.Status.ACTIVE&&l.campus.equals(user.campus())).map(this::listingResponse).toList();}
    @PutMapping("/me/saved/{listingId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveListing(@PathVariable Long listingId,@AuthenticationPrincipal CampusPrincipal user){Listing l=listingInCampus(listingId,user.campus());if(l.status!=Listing.Status.ACTIVE)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Listing unavailable");saved.save(new SavedListing(user.id(),listingId));}
    @DeleteMapping("/me/saved/{listingId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsaveListing(@PathVariable Long listingId,@AuthenticationPrincipal CampusPrincipal user){saved.deleteByUserIdAndListingId(user.id(),listingId);}

    @GetMapping("/me/notifications") @Transactional(readOnly=true)
    public List<ApiModels.NotificationResponse> notifications(@AuthenticationPrincipal CampusPrincipal user){return notifications.findByUserIdOrderByCreatedAtDesc(user.id()).stream().map(ApiModels.NotificationResponse::from).toList();}
    @PostMapping("/me/notifications/read-all") @Transactional
    public void readNotifications(@AuthenticationPrincipal CampusPrincipal user){notifications.findByUserIdOrderByCreatedAtDesc(user.id()).forEach(n->{n.read=true;notifications.save(n);});}
    @PostMapping("/requests/{id}/review") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public ApiModels.ReviewResponse review(@PathVariable Long id,@AuthenticationPrincipal CampusPrincipal user,@Valid @RequestBody ApiModels.ReviewInput in){LoanRequest r=loans.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Request not found"));if(!r.borrowerId.equals(user.id()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the borrower can review this loan");if(r.status!=LoanRequest.Status.RETURNED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Reviews are available after the item is returned");if(reviews.existsByLoanRequestId(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"This loan has already been reviewed");Listing l=listings.findById(r.listingId).orElseThrow();Review savedReview=reviews.save(new Review(id,user.id(),l.ownerId,in.rating(),in.comment()==null?null:in.comment().trim()));return new ApiModels.ReviewResponse(savedReview.id,savedReview.rating,savedReview.comment,savedReview.createdAt);}

    private ApiModels.ListingResponse listingResponse(Listing l){CampusUser owner=findUser(l.ownerId);Double avg=reviews.averageFor(owner.id);long count=reviews.countByRevieweeId(owner.id);return ApiModels.ListingResponse.from(l,owner,avg,count);}
    private ApiModels.LoanResponse loanResponse(LoanRequest r){Listing l=listings.findById(r.listingId).orElseThrow();return loanResponse(r,l,findUser(r.borrowerId));}
    private ApiModels.LoanResponse loanResponse(LoanRequest r,Listing l,CampusUser borrower){return ApiModels.LoanResponse.from(r,l,borrower);}
    private Listing listingInCampus(Long id,String campus){Listing l=listings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Listing not found"));if(!l.campus.equals(campus))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Listing not found");return l;}
    private CampusUser findUser(Long id){return users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));}
    private void validateCategory(String category){if(!CATEGORIES.contains(category))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown category");}
}
