package de.dimond.tippspiel.test

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import scala.util.Random

import de.dimond.tippspiel.model._
import de.dimond.tippspiel.model.PersistanceConfiguration._

class UserRankingTest extends FlatSpec with ShouldMatchers {

  "Ranking empty user set" should "produce empty Seq" in {
    User.rankUsers(Seq()) should be (Seq())
  }

  "Ranking one user" should "produce one user" in {
    val mockUser = new MockUser(1)
    User.rankUsers(Seq(mockUser)) should be (Seq((PrimaryRank(1), mockUser)))
  }

  "Ranking multiple users" should "produce the right ranking" in {
    val mockUser1 = new MockUser(1)
    val mockUser2 = new MockUser(2)
    val mockUser3 = new MockUser(3)
    val mockUser4 = new MockUser(4)
    val mockUser5 = new MockUser(5)
    mockUser5.points = 13
    mockUser3.points = 8
    mockUser1.points = 7
    mockUser2.points = 2
    mockUser4.points = 0
    User.rankUsers(Seq(mockUser1, mockUser2, mockUser3, mockUser4, mockUser5)) should be
      (Seq((PrimaryRank(1), mockUser5),
           (PrimaryRank(2), mockUser3),
           (PrimaryRank(3), mockUser1),
           (PrimaryRank(4), mockUser2),
           (PrimaryRank(5), mockUser4)))
  }

  "Ranking with the same points" should "be stable" in {
    val users = for {
      i <- 1 to 10000
      val mockUser = {
        val newUser = new MockUser(i)
        newUser.points = Random.nextInt(5)
        newUser
      }
    } yield mockUser
    val points4 = users.filter(_.points == 4)
    val points3 = users.filter(_.points == 3)
    val points2 = users.filter(_.points == 2)
    val points1 = users.filter(_.points == 1)
    val points0 = users.filter(_.points == 0)

    User.rankUsers(users).map(_._2) should be (points4 ++ points3 ++ points2 ++ points1 ++ points0)
  }

  "Ranking the same points" should "produce the same ranking" in {
    val users = for(i <- 1 to 10000) yield new MockUser(i)
    users.map(_.points = 10)
    val head = (PrimaryRank(1), users.head)
    val tail = users.tail.map((SecondaryRank(1), _))

    User.rankUsers(users) should be (Seq(head) ++ tail)
  }

  "Ranking" should "be correct" in {
    val user1 = new MockUser(1)
    val user2 = new MockUser(2)
    val user3 = new MockUser(3)
    val user4 = new MockUser(4)
    val user5 = new MockUser(5)
    val user6 = new MockUser(6)
    val user7 = new MockUser(7)
    val user8 = new MockUser(8)
    val user9 = new MockUser(9)
    user3.points = 10
    user6.points = 8
    user1.points = 8
    user7.points = 4
    user5.points = 4
    user9.points = 4
    user4.points = 3
    user2.points = 2
    user8.points = 1

    User.rankUsers(Seq(user1, user2, user3, user4, user5, user6, user7, user8, user9)) should be (
      Seq((PrimaryRank(1), user3),
          (PrimaryRank(2), user1),
          (SecondaryRank(2), user6),
          (PrimaryRank(4), user5),
          (SecondaryRank(4), user7),
          (SecondaryRank(4), user9),
          (PrimaryRank(7), user4),
          (PrimaryRank(8), user2),
          (PrimaryRank(9), user8))
    )
  }
}
