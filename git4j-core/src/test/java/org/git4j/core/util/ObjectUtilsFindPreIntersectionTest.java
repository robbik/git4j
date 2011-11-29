package org.git4j.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.git4j.core.objs.Commit;
import org.git4j.core.repo.InMemoryRepository;
import org.junit.Before;
import org.junit.Test;

public class ObjectUtilsFindPreIntersectionTest {

	private InMemoryRepository repo;

	private static Commit createCommit() {
		Commit commit = new Commit();
		commit.index().put("RANDOM", UUID.randomUUID().toString());

		return commit;
	}

	@Before
	public void before() {
		repo = new InMemoryRepository();
	}

	/**
	 * o - A|B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifAEqualsToB() throws Exception {
		String idA = UUID.randomUUID().toString();
		String idB = idA;

		assertNull(ObjectUtils.findPreIntersection(repo, idA, idB));
	}

	/**
	 * o - B - A
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifAIsAfterB_PathA() throws Exception {
		// store commit B
		Commit commit = createCommit();
		String idB = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idB);

		String idA = repo.store(commit);

		// intersection should be in B
		assertEquals(idA, ObjectUtils.findPreIntersection(repo, idA, idB));
	}

	/**
	 * o - B - A
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifAIsAfterB_PathB() throws Exception {
		// store commit B
		Commit commit = createCommit();
		String idB = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idB);

		String idA = repo.store(commit);

		// intersection should be in B
		assertNull(ObjectUtils.findPreIntersection(repo, idB, idA));
	}

	/**
	 * o - A - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifBIsAfterA_PathA() throws Exception {
		// store commit A
		Commit commit = createCommit();
		String idA = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idA);

		String idB = repo.store(commit);

		// intersection should be in A
		assertNull(ObjectUtils.findPreIntersection(repo, idA, idB));
	}

	/**
	 * o - A - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifBIsAfterA_PathB() throws Exception {
		// store commit A
		Commit commit = createCommit();
		String idA = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idA);

		String idB = repo.store(commit);

		// intersection should be in A
		assertEquals(idB, ObjectUtils.findPreIntersection(repo, idB, idA));
	}

	/**
	 * o - A
	 * 
	 * o - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifAAndBSameLevelNoIntersection_PathA() throws Exception {
		// store commit A
		Commit commit = createCommit();
		String idA = repo.store(commit);

		// store commit B
		commit = createCommit();
		String idB = repo.store(commit);

		// intersection should be in nothing
		assertEquals(idA, ObjectUtils.findPreIntersection(repo, idA, idB));
	}

	/**
	 * o - A
	 * 
	 * o - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifAAndBSameLevelNoIntersection_PathB() throws Exception {
		// store commit A
		Commit commit = createCommit();
		String idA = repo.store(commit);

		// store commit B
		commit = createCommit();
		String idB = repo.store(commit);

		// intersection should be in nothing
		assertEquals(idB, ObjectUtils.findPreIntersection(repo, idB, idA));
	}

	/**
	 * - A / o - C - \ - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifAAndBSameLevel_PathA() throws Exception {
		// store commit C
		Commit commit = createCommit();
		String idC = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idC);

		String idA = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idC);

		String idB = repo.store(commit);

		// intersection should be in C
		assertEquals(idA, ObjectUtils.findPreIntersection(repo, idA, idB));
	}

	/**
	 * - A / o - C - \ - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifAAndBSameLevel_PathB() throws Exception {
		// store commit C
		Commit commit = createCommit();
		String idC = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idC);

		String idA = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idC);

		String idB = repo.store(commit);

		// intersection should be in C
		assertEquals(idB, ObjectUtils.findPreIntersection(repo, idB, idA));
	}

	/**
	 * o - C - A
	 * 
	 * o - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifALongerThanBNoIntersect_PathA() throws Exception {
		// store commit B
		Commit commit = createCommit();
		String idB = repo.store(commit);

		// store commit C
		commit = createCommit();
		String idC = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idC);

		String idA = repo.store(commit);

		// intersection should be in nothing
		assertEquals(idC, ObjectUtils.findPreIntersection(repo, idA, idB));
	}

	/**
	 * o - C - A
	 * 
	 * o - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifALongerThanBNoIntersect_PathB() throws Exception {
		// store commit C
		Commit commit = createCommit();
		String idC = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idC);

		String idA = repo.store(commit);

		// store commit B
		commit = createCommit();
		String idB = repo.store(commit);

		// intersection should be in nothing
		assertEquals(idB, ObjectUtils.findPreIntersection(repo, idB, idA));
	}

	/**
	 * o - C - B
	 * 
	 * o - A
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifBLongerThanANoIntersect_PathA() throws Exception {
		// store commit A
		Commit commit = createCommit();
		String idA = repo.store(commit);

		// store commit C
		commit = createCommit();
		String idC = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idC);

		String idB = repo.store(commit);

		// intersection should be in nothing
		assertEquals(idA, ObjectUtils.findPreIntersection(repo, idA, idB));
	}

	/**
	 * o - C - B
	 * 
	 * o - A
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifBLongerThanANoIntersect_PathB() throws Exception {
		// store commit A
		Commit commit = createCommit();
		String idA = repo.store(commit);

		// store commit C
		commit = createCommit();
		String idC = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idC);

		String idB = repo.store(commit);

		// intersection should be in nothing
		assertEquals(idC, ObjectUtils.findPreIntersection(repo, idB, idA));
	}

	/**
	 * - D - A / o - C - \ - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifALongerThanB_PathA() throws Exception {
		// store commit C
		Commit commit = createCommit();
		String idC = repo.store(commit);

		// store commit D
		commit = createCommit();
		commit.setParent(idC);

		String idD = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idD);

		String idA = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idC);

		String idB = repo.store(commit);

		// intersection should be in C
		assertEquals(idD, ObjectUtils.findPreIntersection(repo, idA, idB));
	}

	/**
	 * - D - A / o - C - \ - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifALongerThanB_PathB() throws Exception {
		// store commit C
		Commit commit = createCommit();
		String idC = repo.store(commit);

		// store commit D
		commit = createCommit();
		commit.setParent(idC);

		String idD = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idD);

		String idA = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idC);

		String idB = repo.store(commit);

		// intersection should be in C
		assertEquals(idB, ObjectUtils.findPreIntersection(repo, idB, idA));
	}

	/**
	 * - A / o - C - \ - D - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifBLongerThanA_PathA() throws Exception {
		// store commit C
		Commit commit = createCommit();
		String idC = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idC);

		String idA = repo.store(commit);

		// store commit D
		commit = createCommit();
		commit.setParent(idC);

		String idD = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idD);

		String idB = repo.store(commit);

		// intersection should be in C
		assertEquals(idA, ObjectUtils.findPreIntersection(repo, idA, idB));
	}

	/**
	 * - A / o - C - \ - D - B
	 * 
	 * @throws Exception
	 */
	@Test
	public void ifBLongerThanA_PathB() throws Exception {
		// store commit C
		Commit commit = createCommit();
		String idC = repo.store(commit);

		// store commit A
		commit = createCommit();
		commit.setParent(idC);

		String idA = repo.store(commit);

		// store commit D
		commit = createCommit();
		commit.setParent(idC);

		String idD = repo.store(commit);

		// store commit B
		commit = createCommit();
		commit.setParent(idD);

		String idB = repo.store(commit);

		// intersection should be in C
		assertEquals(idD, ObjectUtils.findPreIntersection(repo, idB, idA));
	}
}
