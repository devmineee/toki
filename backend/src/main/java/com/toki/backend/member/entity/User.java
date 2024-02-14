package com.toki.backend.member.entity;

import com.toki.backend.auth.dto.Role;
import com.toki.backend.badge.entity.Badge;
import com.toki.backend.common.utils.ConvertUserTag;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity //@Entity: JPA 엔티티 클래스임을 나타내는 어노테이션
@Getter //Lombok 어노테이션으로, Getter, Setter 및 ToString 메소드를 자동으로 생성
@Table(name="member") //@Table: 엔티티가 매핑될 테이블의 정보를 지정하는 어노테이션
@Setter //Lombok 어노테이션으로, Getter, Setter 및 ToString 메소드를 자동으로 생성
@ToString //Lombok 어노테이션으로, Getter, Setter 및 ToString 메소드를 자동으로 생성
@EntityListeners(AuditingEntityListener.class) //엔티티의 이벤트를 수신하는 리스너를 정의하는 어노테이션
@NoArgsConstructor //파라미터가 없는 생성자를 자동으로 생성
public class User {

	@Id //@Id: 엔티티의 기본 키를 나타내는 어노테이션
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	@GeneratedValue(strategy = GenerationType.UUID) //@GeneratedValue: 기본 키의 생성 전략을 지정하는 어노테이션
	String userPk;

	//    배지 정보를 가져오기 위해서 적음.
//	@OneToMany //일대다 관계를 설정하는 어노테이션으로, 한 명의 사용자는 여러 배지를 가질 수 있다.
//	private List<Badge> badges;
	// 이 부분은 주석 처리하였음.

	@Column(nullable = false,unique = true)
	String userId;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false,length = 20)
	Role userRole;

	@Column(nullable = false, length = 40)
	String userName;

	String userNickName;
	@Column(nullable = true)
	String userEmail;
	Integer userTag;
	Integer snsType;

	@Column(updatable = false)
	@CreatedDate // Spring Data JPA에서 제공하는 어노테이션으로, 생성일자와 수정일자를 관리
	LocalDateTime createAt;

	@LastModifiedDate //pring Data JPA에서 제공하는 어노테이션으로, 생성일자와 수정일자를 관리
	LocalDateTime updateAt;
	@GeneratedValue(generator = "0")
	Integer isDelete;
	LocalDateTime deleteAt;
	String birthYear;
	String profileImageUrl;
	String selfInfo; //자기소개를 의미합니다.


	/*
	잘못된 외래키 관계가 정의되어 있습니다.
	user 엔티티의 pk는 userPk이기 때문에 userTag로 외래키 컬럼명을 잡으면 안됩니다.
	 */
	//User와 Badge를 연결하기 위함.
	@ManyToMany(fetch = FetchType.EAGER) //@ManyToMany: 다대다 관계를 정의하는 어노테이션
	@JoinTable(name = "member_badge", //@JoinTable: 연결 테이블을 지정 //user_badges : 연결테이블의 이름(다대다)
			joinColumns = @JoinColumn(name = "user_pk"),// 현재 엔티티의 외래 키 컬럼명 지정
			inverseJoinColumns = @JoinColumn(name = "idx"))// 대상 엔티티의 외래 키 컬럼명 지정
	//유저 테이블의 유저태그를 가져와서 배지테이블의 배지 인덱스와 연결
	private List<Badge> badges = new ArrayList<>(); //배지들을 반환한다.




	@Builder
	public User(String userPk, String userNickName, String birthYear, String profileImageUrl, String userId, Role userRole, String userName, String userEmail, Integer userTag, Integer snsType) {
		this.userPk = userPk;
		this.userId = userId;
		this.userRole = userRole;
		this.userName = userName;
		this.userNickName = userNickName;
		this.userEmail = userEmail;
		this.birthYear=birthYear;
		this.snsType= snsType;
		this.profileImageUrl=profileImageUrl;
		this.userTag = userTag;
	}


	String hexUserTag; //16진수의 유저태그 변환값 필드

	// 10진수로 표현된 유저 태그를 16진수로 변환하여 hexUserTag 필드에 저장하는 메서드
	//(백엔드 10진수 -> 프론트엔드 16진수)
	public void convertToHex(int userTag) {
		this.userTag = userTag; // userTag 필드에 값을 설정
		this.hexUserTag = ConvertUserTag.convertUserTag(userTag);
		// 10진수를 16진수로 변환하여 hexUserTag 필드에 저장
	}

	// 프론트엔드에서 전달된 16진수를 10진수로 변환하는 메서드
	public void convertFromHex(String hexUserTag) {
		this.hexUserTag = hexUserTag;
		this.userTag = ConvertUserTag.convertUserTag(hexUserTag);
	}
//	@ManyToMany
//	private List<Badge> badges=new ArrayList<>();
}
